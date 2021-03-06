/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.sagres.database.model.SGrade
import com.forcetower.sagres.database.model.SGradeInfo
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.database.accessors.GradeWithClassStudent
import timber.log.Timber

@Dao
abstract class GradeDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(grade: Grade): Long

    @Update(onConflict = IGNORE)
    abstract fun update(grade: Grade)

    @Query("SELECT * FROM Grade")
    abstract fun getAllGradesDirect(): List<Grade>

    @Transaction
    @Query("SELECT * FROM Grade WHERE notified = 1")
    abstract fun getCreatedGradesDirect(): List<GradeWithClassStudent>

    @Transaction
    @Query("SELECT * FROM Grade WHERE notified = 2")
    abstract fun getDateChangedGradesDirect(): List<GradeWithClassStudent>

    @Transaction
    @Query("SELECT * FROM Grade WHERE notified = 3")
    abstract fun getPostedGradesDirect(): List<GradeWithClassStudent>

    @Transaction
    @Query("SELECT * FROM Grade WHERE notified = 4")
    abstract fun getChangedGradesDirect(): List<GradeWithClassStudent>

    @Query("UPDATE Grade SET notified = 0")
    abstract fun markAllNotified()

    @Transaction
    open fun putGrades(grades: List<SGrade>, notify: Boolean = true) {
        grades.forEach {
            val code = it.discipline.split("-")[0].trim()

            val clazz = getClass(code, it.semesterId)
            if (clazz != null) {
                val finalScore = it.finalScore
                    .replace(",", ".")
                    .replace("-", "")
                    .replace("*", "")

                val partialMean = it.partialMean
                    .replace(",", ".")
                    .replace("-", "")
                    .replace("*", "")
                val partialScore = partialMean.toDoubleOrNull()
                val score = finalScore.toDoubleOrNull()
                if (score != null)
                    updateClassScore(clazz.uid, score)
                if (partialScore != null)
                    updateClassPartialScore(clazz.uid, partialScore)

                prepareInsertion(clazz, it, notify)
            } else {
                Timber.d("<grades_clazz_404> :: Clazz not found for ${code}_${it.semesterId}")
            }
        }
    }

    @Query("SELECT c.* FROM Class c, Discipline d, Semester s WHERE c.semester_id = s.uid AND c.discipline_id = d.uid AND d.code = :code AND s.sagres_id = :semester")
    protected abstract fun getClass(code: String, semester: Long): Class?

    private fun prepareInsertion(clazz: Class, it: SGrade, notify: Boolean) {
        val values = HashMap<String, SGradeInfo>()
        it.values.forEach { g ->
            var grade = values[g.name]
            if (grade == null) { grade = g } else {
                if (g.hasGrade()) grade = g
                else if (g.hasDate() && grade.hasDate() && g.date != grade.date) grade = g
                else Timber.d("This grade was ignored ${g.name}_${g.grade}")
            }

            values[g.name] = grade
        }

        values.values.forEach { i ->
            val grade = getNamedGradeDirect(clazz.uid, i.name)
            if (grade == null) {
                val notified = if (i.hasGrade()) 3 else 1
                insert(Grade(classId = clazz.uid, name = i.name, date = i.date, notified = if (notify) notified else 0, grade = i.grade))
            } else {
                if (grade.hasGrade() && i.hasGrade() && grade.grade != i.grade) {
                    grade.notified = 4
                    grade.grade = i.grade
                    grade.date = i.date
                } else if (!grade.hasGrade() && i.hasGrade()) {
                    grade.notified = 3
                    grade.grade = i.grade
                    grade.date = i.date
                } else if (!grade.hasGrade() && !i.hasGrade() && grade.date != i.date) {
                    grade.notified = 2
                    grade.date = i.date
                } else {
                    Timber.d("No changes detected between $grade and $i")
                }
                grade.notified = if (notify) grade.notified else 0
                update(grade)
            }
        }
    }

    @Query("UPDATE Class SET partial_score = :partialScore WHERE uid = :classId")
    protected abstract fun updateClassPartialScore(classId: Long, partialScore: Double)

    @Query("UPDATE Class SET final_score = :score WHERE uid = :classId")
    protected abstract fun updateClassScore(classId: Long, score: Double)

    @Query("SELECT * FROM Grade WHERE class_id = :classId AND name = :name")
    protected abstract fun getNamedGradeDirect(classId: Long, name: String): Grade?

    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile
}