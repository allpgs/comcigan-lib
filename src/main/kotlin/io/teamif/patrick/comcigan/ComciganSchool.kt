/*
 * Copyright (C) 2020 PatrickKR
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact me on <mailpatrickkr@gmail.com>
 */

package io.teamif.patrick.comcigan

import io.teamif.patrick.comcigan.ComciganAPI.Companion.json
import java.net.URLEncoder
import java.util.Base64
import kotlin.collections.ArrayList

/**
 * A class representing comcigan school
 *
 * @param name a keyword to search
 * @throws NoSuchElementException when no search results found
 * @throws IllegalArgumentException when more than one results found
 * @see [ComciganAPI.newSchool]
 *
 * @author PatrickKR
 */
@Suppress("MemberVisibilityCanBePrivate")
class ComciganSchool internal constructor(name: String) {
    val schoolName: String
    val schoolCode: String
    private val schoolUrls: List<String>
    lateinit var schoolData: SchoolData.SchoolRawData
        private set
    init {
        val json = "${ComciganAPI.SEARCH_URL}${URLEncoder.encode(name, ComciganAPI.CHARSET)}".json.asJsonObject.getAsJsonArray("학교검색")
        when (json.count()) {
            0 -> throw NoSuchElementException("No schools have been searched by the name passed.")
            1 -> json.first().asJsonArray
            else -> throw IllegalArgumentException("More than one school is searched by the name passed.")
        }.run {
            schoolName = get(2).asString
            schoolCode = get(3).asString
            schoolUrls = listOf(0.schoolUrl, 1.schoolUrl)

            refresh()
        }
    }

    /**
     * Refreshes the data by parsing the Comcigan Website
     */
    fun refresh() {
        val weekArray = ArrayList<SchoolData.SchoolWeekData>()
        schoolUrls.forEach { url ->
            url.json.asJsonObject.run {
                val shortSubjects = getAsJsonArray(ComciganAPI.SUBJECT_ID).mapNotNull { it.asString }
                val longSubjects = getAsJsonArray("긴${ComciganAPI.SUBJECT_ID}").mapNotNull { it.asString }
                val teachers = getAsJsonArray(ComciganAPI.TEACHER_ID).mapNotNull { it.asString }

                val grades = getAsJsonArray(ComciganAPI.DAILY_ID).mapNotNull { it.asJsonArray }.drop(1)
                val gradeArray = ArrayList<SchoolData.SchoolGradeData>()
                grades.forEach { grade ->
                    val classrooms = grade.map { it.asJsonArray }.drop(1)
                    val classroomArray = ArrayList<SchoolData.SchoolClassroomData>()
                    classrooms.forEach { classroom ->
                        val days = classroom.map { it.asJsonArray }.drop(1)
                        val dayArray = ArrayList<SchoolData.SchoolDayData>(days.count())
                        days.forEach { day ->
                            val codes = day.map { it.asString }.filterNot { it == "0" }
                            val codeArray = ArrayList<SchoolData.SchoolPeriodData>(codes.count())
                            codes.forEach { code ->
                                val subject = code.takeLast(2).toInt()
                                val teacher = code.dropLast(2).toInt()
                                codeArray.add(SchoolData.SchoolPeriodData(shortSubjects[subject], longSubjects[subject], teachers[teacher]))
                            }
                            dayArray.add(SchoolData.SchoolDayData(codeArray))
                        }
                        classroomArray.add(SchoolData.SchoolClassroomData(dayArray))
                    }
                    gradeArray.add(SchoolData.SchoolGradeData(classroomArray))
                }
                weekArray.add(SchoolData.SchoolWeekData(gradeArray))
            }
        }
        schoolData = SchoolData.SchoolRawData(weekArray)
    }

    private val Int.schoolUrl: String
        get() {
            return "${ComciganAPI.BASE_URL}?${Base64.getUrlEncoder().encode("${ComciganAPI.PREFIX}${schoolCode}_0_$this".toByteArray()).decodeToString()}"
                .replace("=", "")
        }
}