package com.sadam.thumbtype.mobile

enum class AppScreen { Onboarding, Home, Learn, Practice, Progress, Profile, Privacy, Trainer, Results }
enum class ThumbSide { LEFT, RIGHT, FLEX }
enum class CoachLevel { FULL, MINIMAL, OFF }
enum class TrainingFocus { BALANCED, SPEED, ACCURACY, RHYTHM }
enum class KeyboardLayer { LETTERS, NUMBERS }

data class Lesson(val id:Int,val stage:Int,val title:String,val subtitle:String,val text:String,val skill:String,val xp:Int=40,val timeLimitSeconds:Int?=null,val isPractice:Boolean=false)
data class AppSettings(val darkMode:Boolean=false,val haptics:Boolean=true,val sounds:Boolean=false,val reducedMotion:Boolean=false,val largeText:Boolean=false,val privacyScreenProtection:Boolean=false,val coachLevel:CoachLevel=CoachLevel.FULL)
data class UserProfile(val targetWpm:Int=50,val targetAccuracy:Int=97,val dailyGoalMinutes:Int=10,val focus:TrainingFocus=TrainingFocus.BALANCED,val baselineWpm:Int=0,val baselineAccuracy:Int=0)
data class KeyAggregate(val presses:Int=0,val errors:Int=0,val totalReactionMs:Long=0L){val accuracy:Int get()=if(presses==0)100 else (((presses-errors).toDouble()/presses)*100).toInt().coerceIn(0,100);val averageReactionMs:Int get()=if(presses==0)0 else (totalReactionMs/presses).toInt()}
data class TransitionAggregate(val count:Int=0,val errors:Int=0,val totalMs:Long=0L){val averageMs:Int get()=if(count==0)0 else (totalMs/count).toInt();val accuracy:Int get()=if(count==0)100 else (((count-errors).toDouble()/count)*100).toInt().coerceIn(0,100)}
data class PressEvent(val expected:Char,val entered:Char,val correct:Boolean,val elapsedFromPreviousMs:Long,val recommended:ThumbSide,val touchSide:ThumbSide)
data class SessionResult(val title:String,val rawWpm:Int,val netWpm:Int,val accuracy:Int,val rhythm:Int,val consistency:Int,val thumbTechnique:Int,val thumbBalance:Int,val thumbScore:Int,val mistakes:Int,val correctedErrors:Int,val chars:Int,val durationMs:Long,val leftTouches:Int,val rightTouches:Int,val weakKey:Char?,val weakTransition:String?,val keyUpdates:Map<Char,KeyAggregate>,val transitionUpdates:Map<String,TransitionAggregate>)
data class HistoryEntry(val epochMs:Long,val wpm:Int,val accuracy:Int,val thumbScore:Int,val minutes:Int,val title:String)
data class PracticeMode(val name:String,val subtitle:String,val text:String,val timeLimitSeconds:Int?=null,val skill:String=name)
data class DailyWorkoutItem(val title:String,val subtitle:String,val minutes:Int,val lesson:Lesson)
