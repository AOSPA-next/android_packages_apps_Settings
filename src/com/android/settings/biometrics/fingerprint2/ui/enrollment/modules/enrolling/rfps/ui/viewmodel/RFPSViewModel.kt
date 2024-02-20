/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintAction
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Enrollment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update

/** View Model used by the rear fingerprint enrollment fragment. */
class RFPSViewModel(
  private val fingerprintEnrollViewModel: FingerprintEnrollEnrollingViewModel,
  private val navigationViewModel: FingerprintNavigationViewModel,
) : ViewModel() {

  /** Value to indicate if the text view is visible or not */
  private val _textViewIsVisible = MutableStateFlow<Boolean>(false)
  val textViewIsVisible: Flow<Boolean> = _textViewIsVisible.asStateFlow()

  /** Indicates if the icon should be animating or not */
  val shouldAnimateIcon = fingerprintEnrollViewModel.enrollFlowShouldBeRunning

  private val enrollFlow: Flow<FingerEnrollState?> = fingerprintEnrollViewModel.enrollFLow

  /**
   * Enroll progress message with a replay of size 1 allowing for new subscribers to get the most
   * recent state (this is useful for things like screen rotation)
   */
  val progress: Flow<FingerEnrollState.EnrollProgress?> =
    enrollFlow
      .filterIsInstance<FingerEnrollState.EnrollProgress>()
      .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

  /** Clear help message on enroll progress */
  val clearHelpMessage: Flow<Boolean> = progress.map { it != null }

  /** Enroll help message that is only displayed once */
  val helpMessage: Flow<FingerEnrollState.EnrollHelp?> =
    enrollFlow
      .filterIsInstance<FingerEnrollState.EnrollHelp>()
      .shareIn(viewModelScope, SharingStarted.Eagerly, 0)
      .transform { _textViewIsVisible.update { true } }

  /**
   * The error message should only be shown once, for scenarios like screen rotations, we don't want
   * to re-show the error message.
   */
  val errorMessage: Flow<FingerEnrollState.EnrollError?> =
    enrollFlow
      .filterIsInstance<FingerEnrollState.EnrollError>()
      .shareIn(viewModelScope, SharingStarted.Eagerly, 0)

  val didCompleteEnrollment: Flow<Boolean> = progress.filterNotNull().map { it.remainingSteps == 0 }

  /** Indicates if the consumer is ready for enrollment */
  fun readyForEnrollment() {
    fingerprintEnrollViewModel.canEnroll()
  }

  /** Indicates if enrollment should stop */
  fun stopEnrollment() {
    fingerprintEnrollViewModel.stopEnroll()
  }

  fun setVisibility(isVisible: Boolean) {
    _textViewIsVisible.update { isVisible }
  }

  /** Indicates that the user is done with trying to enroll a fingerprint */
  fun userClickedStopEnrollDialog() {
    navigationViewModel.update(
      FingerprintAction.USER_CLICKED_FINISH,
      navStep,
      "${TAG}#userClickedStopEnrollingDialog",
    )
  }

  /** Indicates that the application went to the background. */
  fun didGoToBackground() {
    navigationViewModel.update(
      FingerprintAction.DID_GO_TO_BACKGROUND,
      navStep,
      "${TAG}#didGoToBackground",
    )
    stopEnrollment()
  }

  /** Indicates the negative button has been clicked */
  fun negativeButtonClicked() {
    navigationViewModel.update(
      FingerprintAction.NEGATIVE_BUTTON_PRESSED,
      navStep,
      "${TAG}negativeButtonClicked",
    )
  }

  fun finishedSuccessfully() {
    navigationViewModel.update(FingerprintAction.NEXT, navStep, "${TAG}#progressFinished")
  }

  class RFPSViewModelFactory(
    private val fingerprintEnrollEnrollingViewModel: FingerprintEnrollEnrollingViewModel,
    private val navigationViewModel: FingerprintNavigationViewModel,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return RFPSViewModel(fingerprintEnrollEnrollingViewModel, navigationViewModel) as T
    }
  }

  companion object {
    private val navStep = Enrollment::class
    private const val TAG = "RFPSViewModel"
  }
}
