package com.company.attendance.presentation.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.presentation.enrollment.theme.EnrollmentColors

/**
 * Thanh cuộn ngang các chip lọc theo phòng ban
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
        items(departments) { dept ->
            val isSelected = (selectedDepartment == dept)
            val chipBg = if (isSelected) EnrollmentColors.Primary else EnrollmentColors.SurfaceContainer
            val chipTextColor = if (isSelected) Color.White else EnrollmentColors.OnSurfaceVariant

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(chipBg)
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
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = dept,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = chipTextColor
                )
            }
        }
    }
}
