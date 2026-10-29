package com.company.attendance.presentation.navigation

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.company.attendance.di.RepositoryModule
import com.company.attendance.domain.model.Employee
import com.company.attendance.domain.model.FaceQualityMetrics
import com.company.attendance.domain.model.defaultFallbackEmployee
import com.company.attendance.presentation.attendance.AttendanceScreen
import com.company.attendance.presentation.attendance.KioskScreen
import com.company.attendance.presentation.enrollment.EmployeeDetailScreen
import com.company.attendance.presentation.enrollment.EmployeeSearchScreen
import com.company.attendance.presentation.enrollment.FaceCaptureScreen
import com.company.attendance.presentation.enrollment.FaceReviewScreen
import kotlinx.coroutines.launch

/**
 * Điều phối viên điều hướng gốc (Root Navigation Container) cho Kiosk TECNIX.
 * Tách biệt hoàn toàn việc định tuyến màn hình ra khỏi UI của AttendanceScreen.
 */
@Composable
fun KioskApp(
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(KioskScreen.ATTENDANCE) }
    var selectedEmployeeId by remember { mutableStateOf<String?>(null) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var faceQualityMetrics by remember { mutableStateOf<FaceQualityMetrics?>(null) }
    var shouldRefreshEmployeeList by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val employeeRepository = remember { RepositoryModule.employeeRepository }

    when (currentScreen) {
        KioskScreen.ATTENDANCE -> {
            AttendanceScreen(
                onNavigateToEmployeeSearch = {
                    selectedEmployeeId = null
                    selectedEmployee = null
                    capturedBitmap = null
                    faceQualityMetrics = null
                    currentScreen = KioskScreen.EMPLOYEE_SEARCH
                },
                modifier = modifier
            )
        }

        KioskScreen.EMPLOYEE_SEARCH -> {
            EmployeeSearchScreen(
                onNavigateBack = {
                    selectedEmployeeId = null
                    selectedEmployee = null
                    capturedBitmap = null
                    faceQualityMetrics = null
                    currentScreen = KioskScreen.ATTENDANCE
                },
                onEmployeeSelected = { employee: Employee ->
                    selectedEmployee = employee
                    selectedEmployeeId = employee.id.ifBlank { employee.code }
                    capturedBitmap = null
                    faceQualityMetrics = null
                    currentScreen = KioskScreen.EMPLOYEE_DETAIL
                },
                shouldRefresh = shouldRefreshEmployeeList,
                onRefreshConsumed = {
                    shouldRefreshEmployeeList = false
                },
                modifier = modifier
            )
        }

        KioskScreen.EMPLOYEE_DETAIL -> {
            val activeEmployee = selectedEmployee ?: defaultFallbackEmployee
            val empId = selectedEmployeeId ?: activeEmployee.id.ifBlank { activeEmployee.code }

            androidx.compose.runtime.key(empId) {
                EmployeeDetailScreen(
                    employeeId = empId,
                    capturedBitmap = capturedBitmap,
                    forceRefresh = shouldRefreshEmployeeList,
                    onNavigateBack = {
                        selectedEmployeeId = null
                        selectedEmployee = null
                        capturedBitmap = null
                        faceQualityMetrics = null
                        currentScreen = KioskScreen.EMPLOYEE_SEARCH
                    },
                    onStartFaceCapture = { targetEmp: Employee ->
                        selectedEmployee = targetEmp
                        selectedEmployeeId = targetEmp.id.ifBlank { targetEmp.code }
                        capturedBitmap = null
                        faceQualityMetrics = null
                        currentScreen = KioskScreen.FACE_CAPTURE
                    },
                    modifier = modifier
                )
            }
        }

        KioskScreen.FACE_CAPTURE -> {
            val activeEmployee = selectedEmployee ?: defaultFallbackEmployee
            val empId = selectedEmployeeId ?: activeEmployee.id.ifBlank { activeEmployee.code }

            androidx.compose.runtime.key(empId) {
                FaceCaptureScreen(
                    employee = activeEmployee,
                    onNavigateBack = {
                        capturedBitmap = null
                        faceQualityMetrics = null
                        currentScreen = KioskScreen.EMPLOYEE_DETAIL
                    },
                    onCaptureFace = { bitmap, metrics, avatarUrl ->
                        capturedBitmap = bitmap
                        faceQualityMetrics = metrics
                        if (!avatarUrl.isNullOrBlank()) {
                            selectedEmployee = activeEmployee.copy(
                                avatarUrl = avatarUrl,
                                isFaceEnrolled = true,
                                hasFaceSample = true
                            )
                        }
                        currentScreen = KioskScreen.FACE_REVIEW
                    },
                    modifier = modifier
                )
            }
        }

        KioskScreen.FACE_REVIEW -> {
            val activeEmployee = selectedEmployee ?: defaultFallbackEmployee
            val empId = selectedEmployeeId ?: activeEmployee.id.ifBlank { activeEmployee.code }

            androidx.compose.runtime.key(empId) {
                FaceReviewScreen(
                    employee = activeEmployee,
                    capturedBitmap = capturedBitmap,
                    qualityMetrics = faceQualityMetrics ?: FaceQualityMetrics(),
                    onNavigateBack = {
                        capturedBitmap = null
                        faceQualityMetrics = null
                        currentScreen = KioskScreen.EMPLOYEE_DETAIL
                    },
                    onRetakeSample = {
                        coroutineScope.launch {
                            employeeRepository.deleteFaceSamples(empId)
                            employeeRepository.clearDetailCache(empId)
                        }
                        capturedBitmap = null
                        faceQualityMetrics = null
                        shouldRefreshEmployeeList = true
                        currentScreen = KioskScreen.FACE_CAPTURE
                    },
                    onConfirmEnrollment = { id ->
                        coroutineScope.launch {
                            employeeRepository.clearDetailCache(id)
                            val updated = employeeRepository.getEmployeeById(id).getOrNull()
                            selectedEmployee = updated ?: activeEmployee.copy(
                                isFaceEnrolled = true,
                                hasFaceSample = true
                            )
                            shouldRefreshEmployeeList = true
                            currentScreen = KioskScreen.EMPLOYEE_DETAIL
                        }
                    },
                    modifier = modifier
                )
            }
        }
    }
}
