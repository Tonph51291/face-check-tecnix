package com.company.attendance.presentation.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thanh cuộn ngang các chip lọc phòng ban chuẩn thiết kế Stitch.
 * - Active: Nền Brand Blue (Primary), chữ trắng, icon checkmark.
 * - Inactive: Nền trắng, viền slate-200/80, đổ bóng nhẹ.
 */
@Composable
fun DepartmentFilterChips(
    departments: List<String>,
    selectedDepartment: String,
    onDepartmentSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(departments, key = { it }) { dept ->
            val isSelected = (selectedDepartment == dept)

            Row(
                modifier = Modifier
                    .shadow(
                        elevation = if (isSelected) 1.dp else 0.5.dp,
                        shape = CircleShape,
                        spotColor = Color(0x0F0F172A)
                    )
                    .clip(CircleShape)
                    .background(if (isSelected) EnrollmentColors.Primary else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) EnrollmentColors.PrimaryDark.copy(alpha = 0.2f)
                                else EnrollmentColors.OutlineVariant.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
                    .clickable { onDepartmentSelected(dept) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = dept,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color.White else EnrollmentColors.OnSurfaceSecondary
                )
            }
        }
    }
}
