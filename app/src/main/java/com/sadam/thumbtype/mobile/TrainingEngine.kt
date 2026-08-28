package com.sadam.thumbtype.mobile

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object TrainingEngine {
    private const val LEFT_FIXED="qwerasdfzxcv"
    private const val RIGHT_FIXED="uiopjklm"
    private const val CENTER="tyghbn"

    fun fixedZone(char:Char):ThumbSide{val c=char.lowercaseChar();return when{c in LEFT_FIXED->ThumbSide.LEFT;c in RIGHT_FIXED->ThumbSide.RIGHT;c in CENTER||c==' '->ThumbSide.FLEX;c.isDigit()->if(c in '0'..'5')ThumbSide.LEFT else ThumbSide.RIGHT;c in "`~!@#$%^"->ThumbSide.LEFT;c in "&*()-_=+[]{}\\|;:'\",.<>/?"->ThumbSide.RIGHT;else->ThumbSide.FLEX}}

    fun recommendedThumb(char:Char,previousChar:Char?,nextChar:Char?,previousRecommended:ThumbSide?):ThumbSide{
        val fixed=fixedZone(char);if(fixed!=ThumbSide.FLEX)return fixed
        if(char==' ')return when(previousRecommended){ThumbSide.LEFT->ThumbSide.RIGHT;ThumbSide.RIGHT->ThumbSide.LEFT;else->ThumbSide.FLEX}
        val natural=when(char.lowercaseChar()){'t','g','b'->ThumbSide.LEFT;'y','h','n'->ThumbSide.RIGHT;else->ThumbSide.FLEX};if(natural==ThumbSide.FLEX)return ThumbSide.FLEX
        val nextFixed=nextChar?.let(::fixedZone);val prevFixed=previousChar?.let(::fixedZone)
        fun cost(candidate:ThumbSide):Double{var score=0.0;if(candidate!=natural)score+=0.45;if(candidate==previousRecommended)score+=1.15;if(candidate==prevFixed&&prevFixed!=ThumbSide.FLEX)score+=0.35;if(candidate==nextFixed&&nextFixed!=ThumbSide.FLEX)score+=0.28;return score}
        return if(cost(ThumbSide.LEFT)<=cost(ThumbSide.RIGHT))ThumbSide.LEFT else ThumbSide.RIGHT
    }

    fun requiredLayer(char:Char):KeyboardLayer{val c=char.lowercaseChar();return if(c.isLetter()||char==' '||char in ",.?!'\"")KeyboardLayer.LETTERS else KeyboardLayer.NUMBERS}

    fun rhythmScore(intervals:List<Long>):Int{val usable=intervals.filter{it in 35..2500};if(usable.size<3)return 100;val mean=usable.average();if(mean<=0.0)return 100;val d=usable.map{abs(it-mean)}.average();return(100.0-d/mean*115.0).roundToInt().coerceIn(20,100)}
    fun consistencyScore(intervals:List<Long>):Int{val usable=intervals.filter{it in 35..2500};if(usable.size<3)return 100;val mean=usable.average();if(mean<=0.0)return 100;val variance=usable.map{(it-mean)*(it-mean)}.average();return(100.0-sqrt(variance)/mean*95.0).roundToInt().coerceIn(15,100)}

    fun calculateResult(title:String,events:List<PressEvent>,durationMs:Long,targetWpm:Int):SessionResult{
        val safeDuration=durationMs.coerceAtLeast(1L);val minutes=safeDuration/60000.0;val correctEvents=events.filter{it.correct};val correctChars=correctEvents.size;val mistakes=events.count{!it.correct};val total=events.size.coerceAtLeast(1)
        val rawWpm=((correctChars/5.0)/minutes).roundToInt().coerceAtLeast(0);val errorPenalty=if(minutes>0)(mistakes/minutes).roundToInt()else 0;val netWpm=(rawWpm-errorPenalty).coerceAtLeast(0);val accuracy=((correctChars.toDouble()/total)*100).roundToInt().coerceIn(0,100)
        val intervals=correctEvents.drop(1).map{it.elapsedFromPreviousMs};val rhythm=rhythmScore(intervals);val consistency=consistencyScore(intervals)
        val techniqueEvents=correctEvents.filter{it.recommended!=ThumbSide.FLEX&&it.touchSide!=ThumbSide.FLEX};val technique=if(techniqueEvents.isEmpty())100 else(techniqueEvents.count{it.recommended==it.touchSide}*100.0/techniqueEvents.size).roundToInt().coerceIn(0,100)
        val left=correctEvents.count{it.touchSide==ThumbSide.LEFT};val right=correctEvents.count{it.touchSide==ThumbSide.RIGHT};val sideTotal=(left+right).coerceAtLeast(1);val balance=(100.0-abs(left-right)*100.0/sideTotal).roundToInt().coerceIn(0,100)
        val speedScore=(netWpm*100.0/targetWpm.coerceAtLeast(20)).roundToInt().coerceIn(0,100);val overall=(speedScore*.26+accuracy*.29+rhythm*.16+consistency*.11+technique*.13+balance*.05).roundToInt().coerceIn(0,100)
        val keyMap=linkedMapOf<Char,KeyAggregate>();correctEvents.forEach{e->val key=e.expected.lowercaseChar();val old=keyMap[key]?:KeyAggregate();keyMap[key]=old.copy(presses=old.presses+1,totalReactionMs=old.totalReactionMs+e.elapsedFromPreviousMs)};events.filter{!it.correct}.forEach{e->val key=e.expected.lowercaseChar();val old=keyMap[key]?:KeyAggregate();keyMap[key]=old.copy(errors=old.errors+1)}
        val transitionMap=linkedMapOf<String,TransitionAggregate>();correctEvents.zipWithNext().forEach{(a,b)->val pair="${a.expected.lowercaseChar()}${b.expected.lowercaseChar()}";val old=transitionMap[pair]?:TransitionAggregate();transitionMap[pair]=old.copy(count=old.count+1,totalMs=old.totalMs+b.elapsedFromPreviousMs)}
        val weakKey=keyMap.entries.filter{it.key.isLetterOrDigit()}.maxByOrNull{it.value.errors*700+it.value.averageReactionMs}?.key;val weakTransition=transitionMap.entries.maxByOrNull{it.value.errors*800+it.value.averageMs}?.key
        return SessionResult(title,rawWpm,netWpm,accuracy,rhythm,consistency,technique,balance,overall*10,mistakes,mistakes,correctChars,safeDuration,left,right,weakKey,weakTransition,keyMap,transitionMap)
    }

    fun generateWeakDrill(keyStats:Map<Char,KeyAggregate>,transitions:Map<String,TransitionAggregate>):String{
        val weakKeys=keyStats.entries.sortedByDescending{it.value.errors*600+it.value.averageReactionMs}.map{it.key}.filter{it.isLetter()}.take(4);val weakPairs=transitions.entries.sortedByDescending{it.value.errors*700+it.value.averageMs}.map{it.key}.filter{it.length==2&&it.all(Char::isLetter)}.take(4)
        if(weakKeys.isEmpty()&&weakPairs.isEmpty())return"bring the thing tonight then begin another bright thought with steady rhythm"
        val bank=listOf("bring","thing","night","better","great","quick","message","typing","rhythm","today","between","another","bright","right","going","home","train","focus","practice","mobile","smooth","accurate","steady","thumb","reach","center");val chosen=bank.filter{w->weakKeys.any{it in w}||weakPairs.any{it in w}}.take(14);return(if(chosen.size>=6)chosen else chosen+bank.take(10)).distinct().joinToString(" ")
    }

    fun coachingInsight(result:SessionResult,targetAccuracy:Int):String=when{result.accuracy<targetAccuracy-5->"Slow down slightly. Accuracy is the fastest path to reliable speed.";result.thumbTechnique<70->"Your screen-side reach pattern is inconsistent. Follow the live reach cue for center keys.";result.rhythm<72->"Your biggest opportunity is rhythm. Aim for smaller, more even gaps between key presses.";result.consistency<70->"Your pace changes sharply during the session. Keep the same comfortable tempo for longer.";result.netWpm>=50&&result.accuracy>=targetAccuracy->"Excellent control. Increase difficulty rather than forcing more speed on easy text.";result.weakTransition!=null->"Next focus: ${result.weakTransition.uppercase()} transition. ThumbType will emphasize it in Weakness Trainer.";else->"Good foundation. Keep accuracy high and let speed rise from smoother transitions."}
}
