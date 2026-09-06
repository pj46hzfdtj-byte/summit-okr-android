package com.summitokr.android.core

object Routes {
    const val LOGIN = "login"
    const val SUMMARY = "summary"
    const val GOALS = "goals"
    const val TASKS = "tasks"
    const val ME = "me"
    const val GOAL_DETAIL = "objectives/{id}"
    const val FOCUS = "focus"
    const val REVIEWS = "reviews"
    const val VISIONS = "visions"
    const val GANTT = "gantt"
    const val AI = "ai"
    const val RECYCLE = "recycle"
    const val HELP = "help"
    const val NOTIFICATIONS = "notifications"
    fun goalDetail(id: String) = "objectives/$id"
}