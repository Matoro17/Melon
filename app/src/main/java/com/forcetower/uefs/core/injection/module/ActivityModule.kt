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

package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.LauncherActivity
import com.forcetower.uefs.easter.twofoureight.Game2048Activity
import com.forcetower.uefs.feature.about.AboutActivity
import com.forcetower.uefs.feature.barrildeboa.HourglassActivity
import com.forcetower.uefs.feature.demand.DemandActivity
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
import com.forcetower.uefs.feature.home.HomeActivity
import com.forcetower.uefs.feature.login.LoginActivity
import com.forcetower.uefs.feature.profile.ProfileActivity
import com.forcetower.uefs.feature.reminders.RemindersActivity
import com.forcetower.uefs.feature.settings.SettingsActivity
import com.forcetower.uefs.feature.setup.SetupActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract fun bindLauncherActivity(): LauncherActivity
    @ContributesAndroidInjector(modules = [LoginModule::class])
    abstract fun bindLoginActivity(): LoginActivity
    @ContributesAndroidInjector(modules = [SetupModule::class])
    abstract fun bindSetupActivity(): SetupActivity
    @ContributesAndroidInjector(modules = [HomeModule::class])
    abstract fun bindHomeActivity(): HomeActivity
    @ContributesAndroidInjector(modules = [AboutModule::class])
    abstract fun bindAboutActivity(): AboutActivity
    @ContributesAndroidInjector(modules = [DisciplineModule::class])
    abstract fun bindDisciplineDetailsActivity(): DisciplineDetailsActivity
    @ContributesAndroidInjector(modules = [ProfileModule::class])
    abstract fun bindProfileActivity(): ProfileActivity
    @ContributesAndroidInjector(modules = [RemindersModule::class])
    abstract fun bindRemindersActivity(): RemindersActivity
    @ContributesAndroidInjector(modules = [SettingsModule::class])
    abstract fun bindSettingsActivity(): SettingsActivity
    @ContributesAndroidInjector(modules = [DemandModule::class])
    abstract fun bindDemandActivity(): DemandActivity
    @ContributesAndroidInjector(modules = [HourglassModule::class])
    abstract fun bindHourglassActivity(): HourglassActivity
    @ContributesAndroidInjector
    abstract fun bindGame2048Activity(): Game2048Activity
}