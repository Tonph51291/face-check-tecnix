package com.company.attendance.domain.usecase

import android.graphics.Bitmap
import com.company.attendance.core.logging.logD
import com.company.attendance.core.logging.logE
import com.company.attendance.core.logging.logI
import com.company.attendance.core.result.AppError
import com.company.attendance.core.result.Result
import com.company.attendance.domain.ml.FaceEmbedder
import com.company.attendance.domain.model.AttendanceRecord
import com.company.attendance.domain.model.FaceTrackItem
import com.company.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ProcessAttendanceUseCase: Đảm nhận toàn bộ nghiệp vụ chấm công từ khung hình:
 * 1. Cắt khuôn mặt chính xác từ Bitmap nguồn và giải phóng ngay Bitmap lớn 8MB.
 * 2. Trích xuất vector đặc trưng 512D On-Device bằng FaceNet TFLite (Dispatchers.Default).
 * 3. Gửi DUY NHẤT vector 512D lên Backend Golang (imageBase64 = null, không tốn băng thông).
 * 4. Quản lý dọn dẹp sạch sẽ tài nguyên Bitmap trong khối finally.
 */
class ProcessAttendanceUseCase(
    private val faceEmbedder: FaceEmbedder,
    private val attendanceRepository: AttendanceRepository
) {

    suspend fun execute(
        sourceBitmap: Bitmap,
        targetFace: FaceTrackItem,
        sensorWidth: Int,
        sensorHeight: Int,
        isFrontCamera: Boolean
    ): Result<AttendanceRecord> = withContext(Dispatchers.Default) {
        var croppedFace: Bitmap? = null

        try {
            // 1. Ánh xạ tọa độ khuôn mặt sang kích thước Bitmap thực tế
            val exactBox = faceEmbedder.mapBoundingBoxToBitmap(
                sensorBox = targetFace.boundingBox,
                sensorWidth = sensorWidth,
                sensorHeight = sensorHeight,
                bitmapWidth = sourceBitmap.width,
                bitmapHeight = sourceBitmap.height,
                isFrontCamera = isFrontCamera
            )

            // 2. Cắt khuôn mặt với viền lề an toàn 20%
            croppedFace = faceEmbedder.cropFace(sourceBitmap, exactBox, marginRatio = 0.20f)
            if (croppedFace != null && croppedFace != sourceBitmap) {
                // Giải phóng ngay lập tức Bitmap toàn màn hình 8MB
                sourceBitmap.recycle()
            }

            val faceToInfer = croppedFace ?: sourceBitmap

            // 3. Trích xuất vector 512D chuẩn hóa L2 bằng FaceNet TFLite
            val faceVector = faceEmbedder.extractEmbeddingFromCroppedFace(
                croppedFace = faceToInfer,
                rotationAngleZ = targetFace.headEulerAngleZ
            )

            if (faceVector == null) {
                logE("[PROCESS_ATTENDANCE] Không thể trích xuất vector khuôn mặt On-Device")
                return@withContext Result.Error(AppError.Unknown("Không thể trích xuất đặc trưng khuôn mặt."))
            }

            logI("[PROCESS_ATTENDANCE] Gửi vector 512D (dim=${faceVector.size}) lên FaceCheck Backend, không gửi ảnh Base64")

            // 4. Gửi duy nhất Vector 512D lên Backend Golang, tuyệt đối không gửi ảnh
            val result = attendanceRepository.checkIn(
                faceVector = faceVector.toList(),
                imageBase64 = null
            )

            result
        } catch (e: Exception) {
            logE("[PROCESS_ATTENDANCE] Lỗi ngoại lệ trong quá trình xử lý chấm công", e)
            Result.Error(AppError.Unknown(e.localizedMessage ?: "Lỗi xử lý sinh trắc"))
        } finally {
            // Đảm bảo giải phóng hoàn toàn ảnh crop tạm thời
            if (croppedFace != null && !croppedFace.isRecycled) {
                croppedFace.recycle()
            }
        }
    }
}
