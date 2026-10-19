---
name: gpms-domain-knowledge
description: >-
  Sử dụng skill này khi cần hiểu nghiệp vụ quản lý đồ án tốt nghiệp của hệ thống TLU GPMS.
  Ví dụ: khi user hỏi về quy trình bảo vệ, vai trò trong hội đồng, luồng chấm điểm, role nào được làm gì,
  hoặc khi cần thiết kế entity/API liên quan đến nghiệp vụ đồ án (topic, outline, report, council, grading).
---

# Domain Knowledge - Hệ thống Quản lý Đồ án TLU

> Nguồn chi tiết và cập nhật nhất: `../readme.md/Project_Context.md`, `../readme.md/work_flow/`, `../readme.md/case_execution/`.
> Nếu skill này mâu thuẫn với tài liệu trong `readme.md` hoặc với code → tin code và `readme.md`, rồi báo user.
> Mục nào ghi **(chưa chốt)** → PHẢI hỏi user trước khi code, không tự quyết định.

## Tổng quan hệ thống

**TLU GPMS** (Thuy Loi University - Graduation Project Management System) là hệ thống quản lý toàn bộ quy trình đồ án tốt nghiệp, từ đăng ký đề tài đến bảo vệ và chấm điểm.

## Các module nghiệp vụ

### 1. Quản lý Tài khoản & Phân quyền (`account`) ✅ Đã có
- Đăng nhập qua Google OAuth2 (chỉ cho email @tlu.edu.vn hoặc email đã được admin thêm)
- RBAC: Role-Based Access Control với Permission chi tiết (seed trong `DB_UPDATE/DATA_ROLE_PERMISSION.txt`)
- Account → Student (1:1), Account → Teacher (1:1)

### 2. Quản lý Sinh viên & Giáo viên ✅ Đã có (thêm SV đơn lẻ, import Excel, thêm GV)
- Sinh viên: thuộc một Lớp (`className`), một Khóa (`batch`), một Ngành (`Major`)
- Giáo viên: có đơn vị công tác (`organization`), thuộc một Ngành, có giới hạn số SV hướng dẫn (`amount`)
- Cả hai liên kết với `Account` qua `@OneToOne`
- TBM thêm giáo viên → hệ thống gán role `SUPERVISOR`
- Luồng chi tiết: `../readme.md/case_execution/addStudent_TBM.md`, `../readme.md/case_execution/addLecturer_TBM.md`

### 3. Quản lý Đề tài (`topic`)
- Sinh viên đăng ký GVHD (`SUPERVISOR`), đăng ký đề tài kèm theo hoặc sau
- GVHD duyệt / từ chối đơn đăng ký hướng dẫn và tên đề tài
- Trạng thái đề tài / đơn đăng ký: **(chưa chốt)** — hỏi user danh sách enum trước khi code
- Mỗi đề tài có: tên, mô tả, yêu cầu, lĩnh vực nghiên cứu
- Mỗi đề tài gắn với một học kỳ (`Semester`)

### 4. Quản lý Đề cương (`outline`)
- Sinh viên nộp đề cương cho đề tài đã đăng ký
- **Giáo viên nhận xét (`COMMENTING_LECTURER`)** duyệt / từ chối đề cương (từ chối bắt buộc ghi nhận xét)
- GVHD xem đề cương của sinh viên mình hướng dẫn
- Trạng thái dự kiến: `DRAFT → SUBMITTED → (APPROVED | REVISION_REQUIRED)`, `REVISION_REQUIRED` quay lại `SUBMITTED` khi SV nộp lại — **(chưa chốt)**

### 5. Quản lý Báo cáo (`report`)
- Sinh viên nộp báo cáo cuối (final report)
- GVHD xem, duyệt / từ chối báo cáo cuối
- GVHD kiểm tra trùng lặp bằng hệ thống bên ngoài rồi nộp file kết quả kiểm tra trùng lên hệ thống
- GVHD nộp phiếu theo dõi tiến trình sinh viên

### 6. Quản lý Phản biện (`review`)
- **Chủ tịch (`CHAIRPERSON`) hoặc Thư ký (`SECRETARY`)** phân công giáo viên phản biện (`EXAMINER`): chọn 2 trong 5 thành viên hội đồng, không được là GVHD của sinh viên đó
- GVPB xem sinh viên phản biện, tạo lịch phản biện
- GVPB duyệt / từ chối sinh viên đủ điều kiện ra hội đồng (trước buổi bảo vệ) và chấm điểm phản biện

### 7. Quản lý Hội đồng bảo vệ (`defense_council`)
- Hội đồng gồm **đúng 5 thành viên**: 1 Chủ tịch (`CHAIRPERSON`), 1 Thư ký (`SECRETARY`), 3 Ủy viên (`COUNCIL_MEMBER`)
- Trưởng bộ môn (`HEAD_OF_DEPARTMENT`) tạo hội đồng, cập nhật, phân công sinh viên vào hội đồng
- Role hội đồng được gán khi TBM tạo hội đồng

### 8. Quản lý Lịch bảo vệ (`defense_schedule`)
- Lịch bảo vệ: ngày, giờ, phòng, hội đồng, danh sách sinh viên
- Chủ tịch / Thư ký lập thời gian bảo vệ cụ thể (ca của từng sinh viên)
- Đổi lịch: Chủ tịch hoặc Thư ký đề xuất, TBM xác nhận
- Sinh viên xem lịch bảo vệ của mình

### 9. Quản lý Chấm điểm (`grading`)
- TBM cài đặt tiêu chí chấm điểm (hướng dẫn, phản biện, hội đồng)
- GVHD chấm điểm hướng dẫn; GVPB chấm điểm phản biện; cả 5 thành viên hội đồng chấm điểm hội đồng
- Công thức điểm tổng kết và trọng số các thành phần: **(chưa chốt)**
- Thang điểm: 0–10 hoặc theo bảng tiêu chí rubric **(chưa chốt)**

### 10. Thông báo (`notification`) ✅ Đã có
- Real-time qua SSE (Server-Sent Events), luồng chi tiết: `../readme.md/case_execution/notification.md`
- Thông báo do hệ thống tự động gửi khi: duyệt đề tài, phân công phản biện, lịch bảo vệ, kết quả điểm, v.v.

### 11. Quản lý Học kỳ (`semester`) ⚠️ Mới có entity khung
- Entity `Semester` hiện chỉ có `id` và `publicId`
- Mỗi đợt đồ án gắn với một học kỳ; các field (tên, năm học, ngày bắt đầu/kết thúc, trạng thái) **(chưa chốt)** — xem `../readme.md/work_flow/01_KhoiTaoDotDoAn.md` và hỏi user

### 12. AI Service - Hỗ trợ soát kết quả kiểm tra trùng lặp
- **Luồng**: Sinh viên nộp final report → GVHD dùng hệ thống bên ngoài kiểm tra trùng lặp → GVHD upload báo cáo phân tích → AI đọc và tóm tắt báo cáo trùng lặp
- **Mục đích**: Hỗ trợ giáo viên soát lại kết quả kiểm tra đạo văn
- **Tech**: Spring AI (starter `spring-ai-starter-model-openai`)

## Quan hệ giữa các vai trò

Nguồn chuẩn: `common/enums/Role.java`. Không có role nền `TEACHER`/`LECTURER`; một tài khoản giáo viên có thể giữ nhiều role cùng lúc.

### 1. Danh sách role và cách được gán

| Role (enum) | Tên tiếng Việt | Cách có được role |
|---|---|---|
| `ADMIN` | Quản trị viên | Do hệ thống cấp |
| `HEAD_OF_DEPARTMENT` | Trưởng bộ môn (TBM) | Gán cho giáo viên giữ chức Trưởng bộ môn |
| `STUDENT` | Sinh viên | Tự động khi TBM thêm sinh viên vào danh sách làm đồ án của kỳ |
| `SUPERVISOR` | Giáo viên hướng dẫn (GVHD) | Hiện tại: tự động khi TBM thêm giáo viên của kỳ |
| `COMMENTING_LECTURER` | Giáo viên nhận xét (GVNX) | TBM phân công |
| `CHAIRPERSON` | Chủ tịch hội đồng | Khi TBM tạo hội đồng |
| `SECRETARY` | Thư ký hội đồng | Khi TBM tạo hội đồng |
| `COUNCIL_MEMBER` | Ủy viên hội đồng | Khi TBM tạo hội đồng (3 người) |
| `EXAMINER` | Giáo viên phản biện (GVPB) | Chủ tịch hoặc Thư ký phân công, chọn 2 trong 5 thành viên hội đồng |

- Một tài khoản có thể có nhiều role (ví dụ vừa là `SUPERVISOR` vừa là `COUNCIL_MEMBER`).
- Admin có thể thu hồi role của tài khoản vi phạm chính sách của trường.
- Trong `@PreAuthorize` dùng đúng tên enum: `hasRole('SUPERVISOR')`, `hasAnyRole('CHAIRPERSON', 'SECRETARY')`.

### 2. Chức năng chính của từng role

```
ADMIN
  ├── Quản lý tài khoản: xem, thêm, xóa, đổi trạng thái, xem lịch sử hoạt động
  ├── Phân quyền: gán / thu hồi role
  ├── Quản lý học kỳ: xem, thêm, sửa
  └── Quản lý ngành: xem, thêm, xóa (khoa là dữ liệu seed sẵn)

HEAD_OF_DEPARTMENT (Trưởng bộ môn)
  ├── Quản lý sinh viên làm đồ án: thêm, xem, xuất danh sách
  ├── Quản lý giáo viên của kỳ: thêm, xóa, cài đặt số lượng sinh viên mỗi GVHD
  ├── Quản lý lịch trình đồ án: thêm, sửa, xóa
  ├── Cài đặt tiêu chí chấm điểm (hướng dẫn, phản biện, hội đồng)
  ├── Phân công giáo viên hướng dẫn và giáo viên nhận xét
  ├── Quản lý hội đồng: tạo hội đồng, cập nhật, phân công sinh viên vào hội đồng,
  │   xác nhận đổi lịch bảo vệ, xem mọi hội đồng, xuất danh sách
  ├── Quản lý tài liệu mẫu: thêm, sửa, xóa
  ├── Thống kê: sinh viên đỗ đồ án, tỷ lệ GV hướng dẫn sinh viên qua đồ án
  └── Quản lý file quyết định có chữ ký hiệu trưởng (import, lưu trữ)

SUPERVISOR (Giáo viên hướng dẫn)
  ├── Duyệt / từ chối đơn đăng ký hướng dẫn của sinh viên
  ├── Duyệt / từ chối tên đề tài
  ├── Xem sinh viên hướng dẫn và đề cương của họ
  ├── Xem, duyệt / từ chối báo cáo cuối
  ├── Nộp file kết quả kiểm tra trùng báo cáo
  ├── Nộp phiếu theo dõi tiến trình sinh viên
  └── Chấm điểm hướng dẫn

COMMENTING_LECTURER (Giáo viên nhận xét)
  ├── Xem sinh viên nhận xét và đề cương của họ
  └── Duyệt / từ chối đề cương (từ chối bắt buộc ghi nhận xét)

EXAMINER (Giáo viên phản biện)  ← 2 trong 5 thành viên hội đồng, không phải GVHD của sinh viên đó
  ├── Xem sinh viên phản biện
  ├── Tạo lịch phản biện
  ├── Duyệt / từ chối sinh viên đủ điều kiện ra hội đồng (trước buổi bảo vệ)
  └── Chấm điểm phản biện

CHAIRPERSON (Chủ tịch hội đồng)  ← chỉ thao tác trên hội đồng được phân công
  ├── Xem thông tin hội đồng, danh sách sinh viên, kế hoạch bảo vệ
  ├── Lập thời gian bảo vệ cụ thể (ca bảo vệ từng sinh viên)
  ├── Đề xuất đổi lịch bảo vệ (TBM xác nhận)
  ├── Phân công giáo viên phản biện (2 trong 5 thành viên)
  ├── Chấm điểm hội đồng
  └── Xem điểm tổng (final) của 5 thành viên hội đồng

SECRETARY (Thư ký hội đồng)  ← chỉ thao tác trên hội đồng được phân công
  ├── Có toàn bộ quyền của CHAIRPERSON
  └── Quản lý biên bản bảo vệ: thêm, xem, sửa, xóa

COUNCIL_MEMBER (Ủy viên hội đồng)
  ├── Xem thông tin hội đồng, danh sách sinh viên, kế hoạch bảo vệ
  └── Chấm điểm hội đồng

STUDENT (Sinh viên)
  ├── Đăng ký giáo viên hướng dẫn (đăng ký đề tài kèm theo hoặc sau)
  ├── Đăng ký, sửa đề tài
  ├── Nộp đề cương
  ├── Nộp báo cáo cuối
  ├── Xem lịch phản biện, lịch bảo vệ (chỉ của mình)
  └── Xem điểm đồ án (chỉ điểm tổng)
```

- "SECRETARY có toàn bộ quyền của CHAIRPERSON" không phải kế thừa role tự động: khi viết `@PreAuthorize` cho chức năng của Chủ tịch thì dùng `hasAnyRole('CHAIRPERSON', 'SECRETARY')`.

### 3. Ràng buộc nghiệp vụ

- GVHD được ngồi trong hội đồng nhưng không được làm GVPB của sinh viên mình hướng dẫn.
- Hội đồng gồm 5 thành viên: 1 Chủ tịch, 1 Thư ký, 3 Ủy viên. Cả 5 người đều chấm điểm hội đồng.
- Đổi lịch bảo vệ: Chủ tịch hoặc Thư ký đề xuất, TBM xác nhận.
- Điểm thành phần chỉ người chấm xem được. Chủ tịch và Thư ký xem được điểm tổng của 5 thành viên. Sinh viên chỉ xem điểm tổng đồ án.
- GVHD, GVNX, GVPB chỉ thấy sinh viên mình phụ trách. Sinh viên chỉ thấy dữ liệu của chính mình. (Kiểm tra sở hữu trong ServiceImpl bằng `getAccountInfo()` hoặc `@PostAuthorize`.)
- Chủ tịch, Thư ký, Ủy viên chỉ thao tác trên hội đồng mình được phân công.
- Chỉ TBM xem được file quyết định có chữ ký hiệu trưởng.
- Chỉ TBM được thêm, sửa, xóa tài liệu mẫu. Mọi giáo viên và sinh viên chỉ xem.
- Thông báo do hệ thống tự động gửi.

## File Storage
- Hệ thống dùng **Cloudinary** để lưu trữ file (đề cương, báo cáo, tài liệu)
- Config: `ConfigCloudinary.java`; dùng tiện ích trong `common/utils` thay vì gọi SDK trực tiếp
