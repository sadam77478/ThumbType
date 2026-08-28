const lessons=[
 ['Everyday words','hello thanks please okay sure right now good morning see you soon'],
 ['Left-zone control','we are ready. dad saw a red car. fred was very calm.'],
 ['Right-zone control','you know him. join our online room. look up your link.'],
 ['Center transitions','the young human being can move between both thumb zones naturally.'],
 ['Short messages','I am on my way. Please send me the location. I will call you when I arrive.'],
 ['Capital letters','Sadam lives in Loralai. Pakistan has many beautiful places. Today I am practicing mobile typing.'],
 ['Punctuation','First, check the message. Then, correct the mistakes. Finally, send it when everything looks right.'],
 ['Questions','Where are you going? What time will you arrive? Can you send me the details? Yes, I can.'],
 ['Numbers and dates','The meeting is on 28/08/2026 at 10:30 AM. The total is 5,250 PKR and the discount is 15%.'],
 ['Professional message','Hello, I have reviewed the latest changes. Everything looks good on mobile, but two small issues should be fixed before publishing.']
];
const modes={
 zones:'we read fast and stay calm. you look good and type well. both thumbs share the center.',
 alternation:'red jump red jump fast look fast look we move you move we type you type',
 speed:'Mobile typing gets faster when both thumbs keep a steady rhythm and avoid long cross-screen reaches.',
 accuracy:'Accuracy first. Type every word carefully, keep both thumbs relaxed, and avoid unnecessary corrections.',
 numbers:'Order 48291 costs 7,450 PKR. Delivery is expected on 30/08/2026 between 2:00 and 5:00 PM.',
 punctuation:'Wait, are you sure? Yes, I checked it; everything looks fine. Please send it now.'
};

const fixedLeft=new Set('qwerasdfzxcv'.split(''));
const fixedRight=new Set('uiopjklm'.split(''));
const flexLeft=new Set('tgb'.split(''));
const flexRight=new Set('yhn'.split(''));
const rows=[['q','w','e','r','t','y','u','i','o','p'],['a','s','d','f','g','h','j','k','l'],['z','x','c','v','b','n','m']];
let idx=0,start=0,text='',lastAssigned='left';

const input=document.querySelector('#input'),target=document.querySelector('#target'),wpm=document.querySelector('#wpm'),acc=document.querySelector('#acc'),best=document.querySelector('#best'),title=document.querySelector('#lessonTitle'),result=document.querySelector('#result'),nextKey=document.querySelector('#nextKey'),leftPill=document.querySelector('#leftPill'),rightPill=document.querySelector('#rightPill'),leftHand=document.querySelector('#leftHand'),rightHand=document.querySelector('#rightHand'),zoneText=document.querySelector('#zoneText'),zoneHint=document.querySelector('#zoneHint');
let bestVal=+localStorage.getItem('bestWpm')||0;best.textContent=bestVal;

function zoneOf(ch){
 ch=(ch||'').toLowerCase();
 if(fixedLeft.has(ch))return 'left';
 if(fixedRight.has(ch))return 'right';
 if(flexLeft.has(ch)||flexRight.has(ch)||ch===' ')return 'flex';
 return 'neutral';
}

function recommendedThumb(ch,pos){
 ch=(ch||'').toLowerCase();
 if(fixedLeft.has(ch)){lastAssigned='left';return 'left'}
 if(fixedRight.has(ch)){lastAssigned='right';return 'right'}
 if(flexLeft.has(ch)){lastAssigned='left';return 'left'}
 if(flexRight.has(ch)){lastAssigned='right';return 'right'}
 if(ch===' '){
   const prev=text[pos-1]||'';
   const prevZone=zoneOf(prev);
   const choice=prevZone==='left'?'right':prevZone==='right'?'left':lastAssigned==='left'?'right':'left';
   lastAssigned=choice;return choice;
 }
 if(/[0-9]/.test(ch)){const n=+ch;const choice=n<=5?'left':'right';lastAssigned=choice;return choice}
 if(',.!?;:/'.includes(ch)){lastAssigned='right';return 'right'}
 return lastAssigned;
}

function buildKeyboard(){
 rows.forEach((row,i)=>{
   const el=document.querySelector('#row'+(i+1));el.innerHTML='';
   row.forEach(k=>{
     const key=document.createElement('div');key.className='key';key.dataset.key=k;key.textContent=k.toUpperCase();
     const z=zoneOf(k);key.classList.add(z==='left'?'left-zone':z==='right'?'right-zone':'flex-zone');el.appendChild(key);
   });
 });
}

function renderTarget(){
 const typed=input.value;
 target.innerHTML='';
 for(let i=0;i<text.length;i++){
   const s=document.createElement('span');s.textContent=text[i];
   if(i<typed.length)s.className=typed[i]===text[i]?'done':'wrong';
   else if(i===typed.length)s.className='current-char';
   else s.className='pending';
   target.appendChild(s);
 }
}

function updateGuide(){
 const pos=input.value.length;
 const ch=text[pos]??'';
 document.querySelectorAll('.key').forEach(k=>k.classList.remove('current'));
 leftPill.classList.remove('active');rightPill.classList.remove('active');leftHand.classList.remove('active');rightHand.classList.remove('active');
 if(!ch){nextKey.textContent='✓';zoneText.textContent='Drill complete';zoneHint.textContent='Great rhythm — restart or continue';return}
 const display=ch===' '?'␠':ch.toUpperCase();nextKey.textContent=display;
 const z=zoneOf(ch),thumb=recommendedThumb(ch,pos);
 const key=document.querySelector(`.key[data-key="${ch.toLowerCase().replace('"','\\"')}"]`);
 if(key)key.classList.add('current');
 if(ch===' ')document.querySelector('.space-key').classList.add('current');else document.querySelector('.space-key').classList.remove('current');
 if(thumb==='left'){leftPill.classList.add('active');leftHand.classList.add('active')}else{rightPill.classList.add('active');rightHand.classList.add('active')}
 if(z==='flex'){
   zoneText.textContent=`Flexible center → ${thumb==='left'?'Left':'Right'} thumb`;
   zoneHint.textContent='Nearest thumb chosen to reduce travel';
 }else if(z==='neutral'){
   zoneText.textContent=`${thumb==='left'?'Left':'Right'} thumb`;
   zoneHint.textContent='Mobile punctuation/number reach';
 }else{
   zoneText.textContent=`${thumb==='left'?'Left':'Right'} reach zone`;
   zoneHint.textContent='Keep the other thumb near its resting area';
 }
}

function load(t,name){
 text=t;title.textContent=name;input.value='';start=0;lastAssigned='left';wpm.textContent='0';acc.textContent='100%';result.textContent='';input.disabled=false;renderTarget();updateGuide();setTimeout(()=>input.focus(),150);
}
function lesson(){load(lessons[idx][1],`Lesson ${idx+1} · ${lessons[idx][0]}`)}
function metrics(){
 const s=input.value;if(!start)return[0,100];let correct=0;for(let i=0;i<s.length;i++)if(s[i]===text[i])correct++;
 const mins=Math.max((Date.now()-start)/60000,1/120);return[Math.round(correct/5/mins),s.length?Math.round(correct/s.length*100):100];
}
input.addEventListener('input',()=>{
 if(!start&&input.value.length)start=Date.now();
 if(input.value.length>text.length)input.value=input.value.slice(0,text.length);
 const m=metrics();wpm.textContent=m[0];acc.textContent=m[1]+'%';renderTarget();updateGuide();
 if(input.value===text){result.textContent='Completed ✓';result.className='ok';if(m[0]>bestVal){bestVal=m[0];best.textContent=bestVal;localStorage.setItem('bestWpm',bestVal)}}else{result.textContent='';result.className=''}
});
document.querySelector('#restart').onclick=()=>load(text,title.textContent);
document.querySelector('#next').onclick=()=>{idx=(idx+1)%lessons.length;lesson()};
document.querySelectorAll('[data-mode]').forEach(b=>b.onclick=()=>load(modes[b.dataset.mode],b.textContent.trim()+' practice'));
buildKeyboard();lesson();