# Kiosk Face Attendance - Kiến Trúc & Quy Ước Dự Án

Hệ thống chấm công bằng khuôn mặt chạy trên Tablet Kiosk (sử dụng nội bộ công ty) với khả năng nhận diện On-Device thời gian thực sử dụng AdaFace (ONNX Runtime Mobile).

---

## 1. Kiến Trúc Tổng Thể (Clean Architecture + MVVM)

Dự án tuân thủ nghiêm ngặt nguyên lý **Clean Architecture** kết hợp mô hình **MVVM (MVI-style UI)**, chia thành 3 tầng cốt lõi:

```
+-------------------------------------------------------------+
|                     Presentation Layer                      |
|          Jetpack Compose | ViewModel | UiState/Event        |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                        Domain Layer                         |
|   UseCases | Repository Interfaces | ML Interfaces | Models |
|                   (Pure Kotlin Module)                      |
+-------------------------------------------------------------+
                              ^
                              |
+-------------------------------------------------------------+
|                         Data Layer                          |
|  Room DB | Retrofit API | ONNX Runtime Mobile | WorkManager |
|                   Repository Implementations                |
+-------------------------------------------------------------+
```

### 1.1. Domain Layer (`com.company.attendance.domain`)
- **Nguyên tắc**: Tầng trung tâm chứa toàn bộ nghiệp vụ (Business Logic). **Tuyệt đối không import Android SDK hay các thư viện bên ngoài**.
- **Model**: Các thực thể thuần túy (`Employee`, `FaceEmbedding`, `AttendanceRecord`, `MatchResult`).
- **Repository Interface**: Định nghĩa các cổng giao tiếp dữ liệu trừu tượng (`EmployeeRepository`, `AttendanceRepository`, `FaceGalleryRepository`).
- **UseCase**: Mỗi UseCase đóng gói đúng 1 hành vi nghiệp vụ đơn lẻ (`RecognizeFaceUseCase`, `SubmitAttendanceUseCase`, `SearchEmployeeUseCase`, `EnrollFaceUseCase`, `SyncGalleryUseCase`).
- **ML Interface**: Định nghĩa các bước trong pipeline nhận diện (`FaceDetector`, `FaceAligner`, `FaceQualityChecker`, `FaceEmbedder`, `LivenessChecker`, `FaceMatcher`).

### 1.2. Data Layer (`com.company.attendance.data`)
- **Local**: Quản lý SQLite qua **Room** (`AppDatabase`, `*Dao`, `*Entity`). Hỗ trợ chế độ hoạt động Offline-First.
- **Remote**: Xử lý giao tiếp mạng qua **Retrofit** (`*Api`, `*Dto`, `*Mapper`).
- **ML Implementation**: Cài đặt thực tế các thuật toán nhận diện:
  - `onnx/`: Chạy suy luận trực tiếp trên thiết bị qua ONNX Runtime Mobile (`OnnxFaceEmbedder` dùng AdaFace, `OnnxFaceDetector`, `CosineFaceMatcher`).
  - `fake/`: Mock data và giả lập ML phục vụ test và preview nhanh không phụ thuộc weights model.
- **Repository Implementation**: Hiện thực hóa các interface từ Domain (`*RepositoryImpl`), điều phối dữ liệu giữa Local Cache và Remote Server.
- **Worker**: Các tác vụ nền chạy bằng **WorkManager** (`SyncGalleryWorker` đồng bộ vector mẫu, `RetryAttendanceWorker` gửi lại các lượt chấm công khi có mạng).

### 1.3. Presentation Layer (`com.company.attendance.presentation`)
- **UI Framework**: Xây dựng 100% bằng **Jetpack Compose** trên tablet kiosk xoay ngang/dọc cố định.
- **MVI-style Pattern**:
  - `*UiState`: Trạng thái bất biến của màn hình.
  - `*UiEvent`: Hành động từ người dùng hoặc hệ thống gửi tới ViewModel.
  - `*UiEffect`: Sự kiện dùng một lần (Single-shot effect) như âm thanh thông báo, Toast, Navigation.
- **Phân chia màn hình**:
  - `attendance/`: Màn hình camera chính, liên tục quét và nhận diện khuôn mặt chấm công.
  - `enrollment/`: Màn hình tìm kiếm nhân viên, chụp ảnh mẫu và gửi đăng ký khuôn mặt mới.
  - `debug/`: Lớp phủ thông tin chẩn đoán kỹ thuật (FPS, thời gian suy luận ONNX, similarity score).
  - `common/`: Theme (Material3) và các component dùng chung (`CameraPreview`, `StatusBanner`, `LoadingIndicator`).

### 1.4. Core & Dependency Injection (`core/`, `di/`)
- `core/`: Chứa cấu hình (`AppConfig`), logging (`AppLogger` bọc Timber), cấu trúc kết quả (`Result<T>`, `AppError`), và các tiện ích xử lý ảnh, ngày giờ.
- `di/`: Các Hilt Module cung cấp dependency (`AppModule`, `NetworkModule`, `DatabaseModule`, `RepositoryModule`, `MlModule`).

---

## 2. Luồng Nghiệp Vụ Chính

### 2.1. Luồng Chấm Công (Attendance Flow)
```
[CameraX Frame] 
      │
      ▼
[FaceDetector] ──(Không có mặt)──► Bỏ qua frame
      │ (Tìm thấy mặt)
      ▼
[FaceAligner & QualityChecker] ──(Mờ / Nghiêng / Tối)──► Báo căn chỉnh lại
      │ (Đạt chuẩn)
      ▼
[LivenessChecker] ──(Giả mạo / Ảnh chụp màn hình)──► Cảnh báo Anti-Spoofing
      │ (Người thật)
      ▼
[FaceEmbedder (AdaFace ONNX)] ──► Trích xuất 512-d Vector
      │
      ▼
[FaceMatcher (Cosine Similarity)] ──► So khớp với Local Gallery
      │
      ├─► [Độ tương đồng >= Ngưỡng] : Xác định nhân viên
      │         │
      │         ▼
      │   [SubmitAttendanceUseCase] ──► Lưu Local DB & Gửi API Chấm Công
      │
      └─► [Độ tương đồng < Ngưỡng]  : Báo không nhận diện được
```

### 2.2. Luồng Đăng Ký Khuôn Mặt Mới (Enrollment Flow)
```
[Giao diện Kiosk] 
      │
      ├─► 1. Nhập tên / mã nhân viên ──► [SearchEmployeeUseCase] (Gọi API tìm kiếm)
      │
      ├─► 2. Hướng dẫn nhân viên vào khung hình ──► [CameraX chụp ảnh mẫu]
      │
      ├─► 3. [FaceEmbedder] trích xuất vector mẫu khuôn mặt
      │
      └─► 4. [EnrollFaceUseCase] ──► Gửi ảnh + vector lên Server ──► Cập nhật Local Gallery
```

---

## 3. Quy Ước Đặt Tên (Naming Conventions)

| Thành phần | Quy ước | Ví dụ |
| :--- | :--- | :--- |
| **Domain Model** | PascalCase, Danh từ | `Employee`, `FaceEmbedding`, `AttendanceRecord` |
| **Domain Repository** | Interface: `[Feature]Repository` | `AttendanceRepository`, `EmployeeRepository` |
| **Domain UseCase** | PascalCase, Động từ + Danh từ + `UseCase` | `RecognizeFaceUseCase`, `SubmitAttendanceUseCase` |
| **ML Interface** | PascalCase, Danh từ mô tả vai trò | `FaceDetector`, `FaceEmbedder`, `FaceMatcher` |
| **ML Implementation** | `[Tech][Role]` hoặc `Fake[Role]` | `OnnxFaceEmbedder`, `FakeFaceEmbedder` |
| **Data Repository** | `[Feature]RepositoryImpl` | `AttendanceRepositoryImpl` |
| **Room Entity** | `[Model]Entity` | `AttendanceEntity`, `EmployeeEntity` |
| **Room DAO** | `[Model]Dao` | `AttendanceDao`, `EmployeeDao` |
| **Retrofit API** | `[Feature]Api` | `AttendanceApi`, `EmployeeApi` |
| **DTO** | `[Action/Entity]Dto` / `[Action]RequestDto` | `AttendanceRequestDto`, `EmployeeDto` |
| **Mapper** | `[Feature]Mapper` | `AttendanceMapper`, `EmployeeMapper` |
| **Worker** | `[Action]Worker` | `SyncGalleryWorker`, `RetryAttendanceWorker` |
| **Screen Composable** | `[Feature]Screen.kt` | `AttendanceScreen`, `EnrollmentScreen` |
| **ViewModel** | `[Feature]ViewModel.kt` | `AttendanceViewModel`, `EnrollmentViewModel` |
| **MVI Contracts** | `[Feature]UiState`, `[Feature]UiEvent`, `[Feature]UiEffect` | `AttendanceUiState`, `AttendanceUiEvent` |
| **Hilt Module** | `[Feature/Layer]Module` | `NetworkModule`, `DatabaseModule`, `MlModule` |
| **Unit Test** | `[TargetClass]Test` | `RecognizeFaceUseCaseTest`, `AttendanceViewModelTest` |
| **Instrumentation Test**| `[TargetClass]Test` | `AttendanceDaoTest`, `AttendanceScreenTest` |

---

## 4. Cấu Trúc Thư Mục Model On-Device
- Thư mục `app/src/main/assets/models/`: Chứa các file trọng số mô hình dạng `.onnx` (như `adaface_ir50.onnx`, `scrfd_face_detector.onnx`).
