package com.company.attendance.domain.ml.tracker

import com.company.attendance.domain.model.FaceTrackItem
import com.company.attendance.presentation.attendance.TrackFaceStatus
import com.company.attendance.presentation.attendance.TrackedFaceUiModel
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * WalkThroughFaceTracker: Chuyên trách quản lý bám vết đa mục tiêu độc lập (Multi-Face Tracking),
 * đánh giá tiêu chuẩn khuôn mặt hợp lệ và điều phối thời gian hồi (Cooldown) chống chấm công lặp.
 * Tách biệt 100% logic theo dõi ra khỏi ViewModel.
 */
class WalkThroughFaceTracker {

    companion object {
        private const val TRACK_COOLDOWN_MS = 12000L   // 12 giây cho track ID người đã chấm đi qua
        private const val EMPLOYEE_COOLDOWN_MS = 300000L // 5 phút cho mã nhân viên chống chấm 2 lần
        private const val MIN_FACE_SIZE_PX = 60         // Kích thước tối thiểu để nhận diện rõ nét
    }

    private val trackStateMap = ConcurrentHashMap<Int, TrackFaceStatus>()
    private val trackEmployeeNames = ConcurrentHashMap<Int, String>()
    private val trackEmployeeCodes = ConcurrentHashMap<Int, String>()
    private val trackCheckInTimes = ConcurrentHashMap<Int, String>()
    private val trackMessages = ConcurrentHashMap<Int, String>()
    private val trackCooldownTimestamps = ConcurrentHashMap<Int, Long>()
    private val employeeCooldownMap = ConcurrentHashMap<String, Long>()
    private val verifyingTrackIds = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    /**
     * Cập nhật danh sách khuôn mặt từ frame camera và chuyển đổi sang danh sách Model hiển thị UI.
     */
    fun updateTracks(detectedFaces: List<FaceTrackItem>): List<TrackedFaceUiModel> {
        val now = System.currentTimeMillis()

        // Tự động dọn dẹp các track ID cũ đã biến mất quá 20 giây
        if (trackCooldownTimestamps.size > 25) {
            trackCooldownTimestamps.entries.removeIf { (now - it.value) > 20000L }
        }

        return detectedFaces.map { face ->
            val trackId = face.trackingId ?: face.boundingBox.hashCode()
            val lastCheckTime = trackCooldownTimestamps[trackId] ?: 0L
            val isVerifying = verifyingTrackIds.contains(trackId)

            val currentStatus = when {
                isVerifying -> TrackFaceStatus.VERIFYING
                trackStateMap[trackId] == TrackFaceStatus.SUCCESS -> {
                    if ((now - lastCheckTime) < 3000L) TrackFaceStatus.SUCCESS else TrackFaceStatus.COOLDOWN
                }
                trackStateMap[trackId] == TrackFaceStatus.DUPLICATE -> TrackFaceStatus.DUPLICATE
                trackStateMap[trackId] == TrackFaceStatus.FAILURE -> {
                    if ((now - lastCheckTime) < 2500L) TrackFaceStatus.FAILURE else TrackFaceStatus.DETECTED
                }
                else -> TrackFaceStatus.DETECTED
            }

            TrackedFaceUiModel(
                trackingId = trackId,
                boundingBox = face.boundingBox,
                status = currentStatus,
                employeeName = trackEmployeeNames[trackId],
                employeeCode = trackEmployeeCodes[trackId],
                checkInTime = trackCheckInTimes[trackId],
                message = trackMessages[trackId]
            )
        }
    }

    /**
     * Tìm khuôn mặt hợp lệ nhất để gửi lên nhận diện (nhìn thẳng, đủ to, chưa bị cooldown và không đang quét).
     */
    fun findEligibleCandidate(detectedFaces: List<FaceTrackItem>): FaceTrackItem? {
        val now = System.currentTimeMillis()

        return detectedFaces.firstOrNull { face ->
            val trackId = face.trackingId ?: face.boundingBox.hashCode()
            val lastCheckTime = trackCooldownTimestamps[trackId] ?: 0L
            val inCooldown = (now - lastCheckTime) < TRACK_COOLDOWN_MS
            val isVerifying = verifyingTrackIds.contains(trackId)

            val isLookingStraight = face.isLookingStraight
            val isSufficientSize = face.boundingBox.width() >= MIN_FACE_SIZE_PX && face.boundingBox.height() >= MIN_FACE_SIZE_PX

            !inCooldown && !isVerifying && isLookingStraight && isSufficientSize
        }
    }

    /** Đánh dấu track bắt đầu được nhận diện (chuyển sang VERIFYING) */
    fun tryClaimForVerification(trackId: Int): Boolean {
        return verifyingTrackIds.add(trackId).also { claimed ->
            if (claimed) {
                trackStateMap[trackId] = TrackFaceStatus.VERIFYING
            }
        }
    }

    /** Hoàn tất quá trình verifying */
    fun releaseVerification(trackId: Int) {
        verifyingTrackIds.remove(trackId)
    }

    /** Ghi nhận chấm công thành công */
    fun markSuccess(
        trackId: Int,
        employeeId: String,
        employeeName: String,
        employeeCode: String,
        checkInTime: String
    ) {
        val now = System.currentTimeMillis()
        trackStateMap[trackId] = TrackFaceStatus.SUCCESS
        trackEmployeeNames[trackId] = employeeName
        trackEmployeeCodes[trackId] = employeeCode
        trackCheckInTimes[trackId] = checkInTime
        trackCooldownTimestamps[trackId] = now
        employeeCooldownMap[employeeId] = now
    }

    /** Ghi nhận chấm công trùng lặp */
    fun markDuplicate(trackId: Int) {
        val now = System.currentTimeMillis()
        trackStateMap[trackId] = TrackFaceStatus.DUPLICATE
        trackCooldownTimestamps[trackId] = now
    }

    /** Ghi nhận chấm công thất bại (khuôn mặt chưa đăng ký hoặc lỗi) */
    fun markFailure(trackId: Int, errorMessage: String) {
        val now = System.currentTimeMillis()
        trackStateMap[trackId] = TrackFaceStatus.FAILURE
        trackMessages[trackId] = errorMessage
        // Cooldown ngắn 3 giây cho người lạ để không gọi server liên tục khi họ đi qua
        trackCooldownTimestamps[trackId] = now - (TRACK_COOLDOWN_MS - 3000L)
    }

    /** Xóa toàn bộ dữ liệu theo dõi */
    fun clear() {
        trackStateMap.clear()
        trackEmployeeNames.clear()
        trackEmployeeCodes.clear()
        trackCheckInTimes.clear()
        trackMessages.clear()
        trackCooldownTimestamps.clear()
        verifyingTrackIds.clear()
    }
}
