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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.company.attendance.domain.model.Employee
import com.company.attendance.presentation.enrollment.components.EmployeeListItemCard
import com.company.attendance.presentation.enrollment.components.EnrollmentTopBar
import com.company.attendance.presentation.enrollment.components.SearchInputBox
import com.company.attendance.presentation.enrollment.components.SupportHelpCard
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Màn hình 3: Danh sách nhân sự & Đăng ký sinh trắc học Kiosk
 * Thiết kế chuẩn pixel-perfect từ Google Stitch (Tailwind slate & brand blue).
 * Kiến trúc MVVM + Jetpack Paging 3 (cachedIn) tối ưu RAM và phản hồi mượt mà.
 */
@Composable
fun EmployeeSearchScreen(
    onNavigateBack: () -> Unit,
    onEmployeeSelected: (Employee) -> Unit = {},
    shouldRefresh: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
    viewModel: EnrollmentViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingEmployees: LazyPagingItems<Employee> = viewModel.employeesPagingFlow.collectAsLazyPagingItems()

    // Tự động làm mới danh sách khi nhận cờ shouldRefresh từ quy trình đăng ký thành công
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            pagingEmployees.refresh()
            onRefreshConsumed()
        }
    }

    val onEventStable = androidx.compose.runtime.remember(viewModel) { { event: EnrollmentUiEvent -> viewModel.onEvent(event) } }
    val onNavigateBackStable = androidx.compose.runtime.remember(onNavigateBack) { onNavigateBack }
    val onEmployeeSelectedStable = androidx.compose.runtime.remember(onEmployeeSelected) { onEmployeeSelected }

    EmployeeSearchContent(
        uiState = uiState,
        pagingEmployees = pagingEmployees,
        onEvent = onEventStable,
        onNavigateBack = onNavigateBackStable,
        onEmployeeSelected = onEmployeeSelectedStable,
        modifier = modifier
    )
}

/**
 * Stateless Content Composable responsive cho cả màn hình Kiosk Tablet và Mobile
 */
@Composable
fun EmployeeSearchContent(
    uiState: EnrollmentUiState,
    pagingEmployees: LazyPagingItems<Employee>,
    onEvent: (EnrollmentUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onEmployeeSelected: (Employee) -> Unit,
    modifier: Modifier = Modifier
) {
    val refreshLoadState = pagingEmployees.loadState.refresh
    val appendLoadState = pagingEmployees.loadState.append
    val isInitialLoading = refreshLoadState is LoadState.Loading
    val isAppendLoading = appendLoadState is LoadState.Loading
    val errorMessage = (refreshLoadState as? LoadState.Error)?.error?.message
        ?: (appendLoadState as? LoadState.Error)?.error?.message

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
                onNavigateBack = onNavigateBack
            )

            // 2. CONTAINER CUỘN RESPONSIVE (GIỮ CÂN ĐỐI TRÊN MỌI KÍCH THƯỚC MÀN HÌNH)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header tiêu đề quy trình & tìm kiếm
                    item(key = "header_section") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Danh sách nhân sự",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EnrollmentColors.OnSurface,
                                letterSpacing = (-0.4).sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Ô tìm kiếm thời gian thực
                            SearchInputBox(
                                query = uiState.searchQuery,
                                onQueryChange = { onEvent(EnrollmentUiEvent.OnSearchQueryChanged(it)) }
                            )



                            // Banner cảnh báo lỗi nếu có
                            if (errorMessage != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(EnrollmentColors.ErrorContainer)
                                        .border(1.dp, EnrollmentColors.WarningTagBorder, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = EnrollmentColors.Error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage,
                                        fontSize = 12.sp,
                                        color = EnrollmentColors.OnErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Thử lại",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EnrollmentColors.PrimaryDark,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EnrollmentColors.PrimaryLight)
                                            .clickable { pagingEmployees.retry() }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Dòng thống kê số lượng & nút đồng bộ làm mới
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
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = EnrollmentColors.Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Tìm thấy ",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = EnrollmentColors.OnSurfaceVariant
                                    )
                                    Text(
                                        text = "${pagingEmployees.itemCount}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EnrollmentColors.Primary
                                    )
                                    Text(
                                        text = " nhân sự",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = EnrollmentColors.OnSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    // Nút đồng bộ danh sách chuẩn Stitch
                                    IconButton(
                                        onClick = { pagingEmployees.refresh() },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(10.dp), spotColor = Color(0x0F0F172A))
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White)
                                            .border(1.dp, EnrollmentColors.OutlineVariant.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                                    ) {
                                        if (isInitialLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(15.dp),
                                                color = EnrollmentColors.Primary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Đồng bộ danh sách",
                                                tint = EnrollmentColors.OnSurfaceSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. DANH SÁCH NHÂN SỰ HOẶC TRẠNG THÁI INITIAL LOADING / EMPTY
                    when {
                        isInitialLoading && pagingEmployees.itemCount == 0 -> {
                            item(key = "initial_loading") {
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
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "Đang kết nối và tải nhân sự từ AI Server...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EnrollmentColors.OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        !isInitialLoading && pagingEmployees.itemCount == 0 && errorMessage == null -> {
                            item(key = "empty_state") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White)
                                            .border(1.dp, EnrollmentColors.OutlineVariant, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = EnrollmentColors.Outline,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Không tìm thấy nhân sự phù hợp",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EnrollmentColors.OnSurface
                                    )
                                    Text(
                                        text = "Thử tìm kiếm với từ khóa khác hoặc bấm nút làm mới để tải lại.",
                                        fontSize = 12.sp,
                                        color = EnrollmentColors.OnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        else -> {
                            items(
                                count = pagingEmployees.itemCount,
                                key = { index -> pagingEmployees.peek(index)?.id?.takeIf { it.isNotBlank() } ?: "employee_key_$index" }
                            ) { index ->
                                val employee = pagingEmployees[index]
                                if (employee != null) {
                                    EmployeeListItemCard(
                                        employee = employee,
                                        onClick = {
                                            onEmployeeSelected(employee)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Cuộn tải trang tiếp theo (Load-more append)
                            if (isAppendLoading) {
                                item(key = "append_loading") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = EnrollmentColors.Primary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }


                }
            }
        }
    }
}
