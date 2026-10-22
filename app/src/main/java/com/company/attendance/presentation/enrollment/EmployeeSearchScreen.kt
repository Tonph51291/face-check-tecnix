package com.company.attendance.presentation.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Màn hình 3: Tìm kiếm nhân sự và Quản lý đăng ký sinh trắc học
 * Chuẩn kiến trúc MVVM (StateFlow + UiEvent + Clean Separation)
 * Kết nối trực tiếp tới Backend FaceCheck AI Service (GET /api/v1/employees)
 */
@Composable
fun EmployeeSearchScreen(
    onNavigateBack: () -> Unit,
    onEmployeeSelected: (String) -> Unit = {},
    viewModel: EnrollmentViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Quan sát UI State phát ra từ ViewModel theo chu kỳ sống màn hình
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
 * Stateless Content Composable:
 * - Nhận toàn bộ dữ liệu qua [uiState]
 * - Gửi mọi tương tác người dùng qua callback [onEvent]
 * - Hoàn toàn không phụ thuộc logic hay framework bên ngoài, dễ dàng Test và Preview
 */
@Composable
fun EmployeeSearchContent(
    uiState: EnrollmentUiState,
    onEvent: (EnrollmentUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onEmployeeSelected: (String) -> Unit,
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
            // 1. THANH ĐIỀU HƯỚNG TRÊN (TOP APP BAR)
            EnrollmentTopBar(
                title = "TECNIX KIOSK",
                subtitle = "Hệ thống nhận diện & đăng ký khuôn mặt",
                badgeText = "FaceCheck AI Engine",
                onNavigateBack = onNavigateBack
            )

            // 2. KHU VỰC NỘI DUNG CHÍNH (LAZY LIST)
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
                        // Badge tiêu đề quy trình
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
                            text = "Danh sách nhân sự",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EnrollmentColors.OnSurface,
                            letterSpacing = (-0.3).sp
                        )

                        Text(
                            text = "Dữ liệu đồng bộ trực tiếp từ AI Backend Service (SFace + YuNet). Tìm kiếm nhân sự để đăng ký khuôn mặt mới.",
                            fontSize = 13.sp,
                            color = EnrollmentColors.OnSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ô tìm kiếm thời gian thực (Debounce 300ms trong ViewModel)
                        SearchInputBox(
                            query = uiState.searchQuery,
                            onQueryChange = { onEvent(EnrollmentUiEvent.OnSearchQueryChanged(it)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bộ lọc danh mục phòng ban
                        DepartmentFilterChips(
                            departments = uiState.departments,
                            selectedDepartment = uiState.selectedDepartment,
                            onDepartmentSelected = { onEvent(EnrollmentUiEvent.OnSelectDepartment(it)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Thông báo lỗi kết nối nếu có (với nút Thử lại)
                        if (uiState.errorMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EnrollmentColors.Error.copy(alpha = 0.12f))
                                    .border(1.dp, EnrollmentColors.Error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = EnrollmentColors.Error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    fontSize = 12.sp,
                                    color = EnrollmentColors.Error,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Thử lại",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EnrollmentColors.Primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EnrollmentColors.Primary.copy(alpha = 0.15f))
                                        .clickable { onEvent(EnrollmentUiEvent.OnRefresh) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Dòng thống kê số lượng nhân viên tìm thấy và nút đồng bộ lại
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

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.selectedDepartment != "Tất cả") {
                                    Text(
                                        text = "Đang lọc: ${uiState.selectedDepartment}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EnrollmentColors.Primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }

                                // Nút đồng bộ / Refresh dữ liệu từ Backend
                                IconButton(
                                    onClick = { onEvent(EnrollmentUiEvent.OnRefresh) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(EnrollmentColors.SurfaceVariant)
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = EnrollmentColors.Primary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Làm mới danh sách",
                                            tint = EnrollmentColors.OnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 3. DANH SÁCH NHÂN VIÊN HOẶC TRẠNG THÁI LOADING / EMPTY
                when {
                    uiState.isLoading && uiState.filteredEmployees.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = EnrollmentColors.Primary,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Đang kết nối và tải nhân sự từ AI Server...",
                                        fontSize = 13.sp,
                                        color = EnrollmentColors.OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    uiState.filteredEmployees.isEmpty() -> {
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
                                    text = "Không tìm thấy nhân sự phù hợp",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EnrollmentColors.OnSurfaceVariant
                                )
                                Text(
                                    text = "Thử tìm kiếm với từ khóa khác hoặc bấm nút làm mới để tải lại.",
                                    fontSize = 12.sp,
                                    color = EnrollmentColors.Outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        items(uiState.filteredEmployees, key = { it.id }) { employee ->
                            EmployeeListItemCard(
                                employee = employee,
                                onClick = {
                                    onEmployeeSelected(employee.code.ifBlank { employee.id })
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 760.dp)
                                    .padding(vertical = 5.dp)
                            )
                        }
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
