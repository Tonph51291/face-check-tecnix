package com.company.attendance.presentation.enrollment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.components.DepartmentFilterChips
import com.company.attendance.presentation.enrollment.components.EmployeeListItemCard
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.SearchInputBox
import com.company.attendance.presentation.enrollment.components.SupportHelpCard
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Màn hình 3: Tìm kiếm nhân viên chuẩn thiết kế Google Stitch (MVVM Refactored)
 * Node ID: 428e3c00c38249afb630dcee3c593605
 */
@Composable
fun EmployeeSearchScreen(
    onNavigateBack: () -> Unit,
    onEmployeeSelected: (Employee) -> Unit = {},
    viewModel: EnrollmentViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmployeeSearchContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onEmployeeSelected = onEmployeeSelected,
        modifier = modifier
    )
}

/**
 * Stateless Content Composable: Nhận state và đẩy event, không chứa logic nghiệp vụ.
 */
@Composable
fun EmployeeSearchContent(
    uiState: EnrollmentUiState,
    onEvent: (EnrollmentUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onEmployeeSelected: (Employee) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = EnrollmentColors.Surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP APP BAR
            EnrollmentTopBar(
                title = "TECNIX KIOSK",
                subtitle = "Hệ thống chấm công khuôn mặt",
                badgeText = "Verification Details",
                onNavigateBack = onNavigateBack
            )

            // 2. BODY CONTENT (Cuộn mượt mà và co giãn theo kích thước màn hình)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                    ) {
                        // Tiêu đề quy trình đăng ký
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = EnrollmentColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "QUY TRÌNH ĐĂNG KÝ SINH TRẮC",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EnrollmentColors.Primary,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Danh sách nhân viên",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EnrollmentColors.OnSurface,
                            letterSpacing = (-0.3).sp
                        )

                        Text(
                            text = "Tìm kiếm hồ sơ nhân sự để đăng ký hoặc cập nhật dữ liệu nhận diện khuôn mặt vào hệ thống Kiosk",
                            fontSize = 13.sp,
                            color = EnrollmentColors.OnSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ô tìm kiếm thời gian thực
                        SearchInputBox(
                            query = uiState.searchQuery,
                            onQueryChange = { onEvent(EnrollmentUiEvent.OnSearchQueryChanged(it)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bộ lọc danh mục phòng ban (Filter Chips)
                        DepartmentFilterChips(
                            departments = uiState.departments,
                            selectedDepartment = uiState.selectedDepartment,
                            onDepartmentSelected = { onEvent(EnrollmentUiEvent.OnSelectDepartment(it)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dòng chỉ số kết quả tìm thấy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                                    contentDescription = null,
                                    tint = EnrollmentColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Tìm thấy ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                                Text(
                                    text = "${uiState.filteredEmployees.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EnrollmentColors.Primary
                                )
                                Text(
                                    text = " nhân sự",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                            }

                            if (uiState.selectedDepartment != "Tất cả") {
                                Text(
                                    text = "Đang lọc: ${uiState.selectedDepartment}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EnrollmentColors.Primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 3. DANH SÁCH NHÂN VIÊN
                if (uiState.filteredEmployees.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 760.dp)
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = EnrollmentColors.Outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Không tìm thấy nhân viên nào phù hợp",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EnrollmentColors.OnSurfaceVariant
                            )
                            Text(
                                text = "Thử tìm kiếm với từ khóa khác hoặc xóa bộ lọc phòng ban.",
                                fontSize = 12.sp,
                                color = EnrollmentColors.Outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    items(uiState.filteredEmployees, key = { it.id }) { employee ->
                        EmployeeListItemCard(
                            employee = employee,
                            onClick = {
                                onEvent(EnrollmentUiEvent.OnSelectEmployee(employee))
                                onEmployeeSelected(employee)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 760.dp)
                                .padding(vertical = 5.dp)
                        )
                    }
                }

                // 4. HỘP TRỢ GIÚP & HỖ TRỢ HR
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SupportHelpCard()
                }
            }
        }
    }
}
