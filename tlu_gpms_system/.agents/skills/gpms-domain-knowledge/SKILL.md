---
name: gpms-domain-knowledge
description: >-
  Sử dụng skill này khi cần hiểu nghiệp vụ quản lý đồ án tốt nghiệp của hệ thống TLU GPMS.
  Ví dụ: khi user hỏi về quy trình bảo vệ, vai trò trong hội đồng, luồng chấm điểm,
  hoặc khi cần thiết kế entity/API liên quan đến nghiệp vụ đồ án.
---

# Domain Knowledge - Hệ thống Quản lý Đồ án TLU

## Tổng quan hệ thống

**TLU GPMS** (Thuy Loi University - Graduation Project Management System) là hệ thống quản lý toàn bộ quy trình đồ án tốt nghiệp, từ đăng ký đề tài đến bảo vệ và chấm điểm.

## Các module nghiệp vụ

### 1. Quản lý Tài khoản & Phân quyền (`account`) ✅ Đã có
- Đăng nhập qua Google OAuth2 (chỉ cho email @tlu.edu.vn hoặc email đã được admin thêm)
- RBAC: Role-Based Access Control với Permission chi tiết
- Account → Student (1:1), Account → Teacher (1:1)

### 2. Quản lý Sinh viên & Giáo viên
- Sinh viên: thuộc một Lớp (`className`), một Khóa (`batch`), một Ngành (`Major`)
- Giáo viên: có đơn vị công tác (`organization`), thuộc một Ngành, có giới hạn số SV hướng dẫn (`amount`)
- Cả hai liên kết với `Account` qua `@OneToOne`

### 3. Quản lý Đề tài (`topic` / `project`)
- Giáo viên đề xuất đề tài → Trưởng bộ môn duyệt
- Sinh viên đăng ký đề tài → Giáo viên hướng dẫn duyệt
- Trạng thái đề tài: PENDING → APPROVED → IN_PROGRESS → COMPLETED / REJECTED
- Mỗi đề tài có: tên, mô tả, yêu cầu, lĩnh vực nghiên cứu
- Mỗi đề tài gắn với một học kỳ (`Semester`)

### 4. Quản lý Đề cương (`outline`)
- Sinh viên nộp đề cương cho đề tài đã đăng ký
- Giáo viên hướng dẫn review và phê duyệt/yêu cầu chỉnh sửa
- Trạng thái: DRAFT → SUBMITTED → APPROVED → REVISION_REQUIRED

### 5. Quản lý Báo cáo (`report`)
- Sinh viên nộp báo cáo tiến độ định kỳ
- Sinh viên nộp báo cáo cuối cùng (final report) để bảo vệ
- Final report được kiểm tra đạo văn (qua hệ thống bên ngoài + AI đọc kết quả)
- Giáo viên đẩy báo cáo phân tích trùng lặp lên hệ thống

### 6. Quản lý Phản biện (`review`)
- Trưởng bộ môn phân công giáo viên phản biện cho mỗi đề tài
- Giáo viên phản biện đọc báo cáo và đưa ra nhận xét
- Nhận xét gồm: điểm mạnh, điểm yếu, câu hỏi chuẩn bị cho bảo vệ

### 7. Quản lý Hội đồng bảo vệ (`defense_council`)
- Thành phần hội đồng: Chủ tịch, Thư ký, Ủy viên (thường 3-5 người)
- Trưởng bộ môn thành lập hội đồng
- Hội đồng gắn với một nhóm đề tài cần bảo vệ

### 8. Quản lý Lịch bảo vệ (`defense_schedule`)
- Lịch bảo vệ: ngày, giờ, phòng, hội đồng, danh sách sinh viên
- Thư ký hội đồng quản lý lịch
- Sinh viên xem lịch bảo vệ của mình

### 9. Quản lý Chấm điểm (`grading`)
- Mỗi thành viên hội đồng chấm điểm riêng
- Giáo viên hướng dẫn chấm điểm quá trình
- Giáo viên phản biện chấm điểm phản biện
- Điểm tổng kết = trung bình có trọng số
- Thang điểm: 0-10 hoặc theo bảng tiêu chí rubric

### 10. Thông báo (`notification`) ✅ Đã có
- Real-time qua SSE (Server-Sent Events)
- Gửi thông báo khi: duyệt đề tài, phân công phản biện, lịch bảo vệ, kết quả điểm, v.v.

### 11. Quản lý Học kỳ (`semester`) ✅ Đã có (entity)
- Mỗi đợt đồ án gắn với một học kỳ
- Học kỳ có: tên, năm học, ngày bắt đầu, ngày kết thúc, trạng thái (ACTIVE/INACTIVE)

### 12. AI Service - Kiểm tra đạo văn
- **Luồng**: Sinh viên nộp final report → Giáo viên dùng hệ thống bên ngoài kiểm tra trùng lặp → Giáo viên upload báo cáo phân tích → AI đọc và phân tích báo cáo trùng lặp
- **Mục đích**: Hỗ trợ giáo viên soát lại kết quả kiểm tra đạo văn
- **Tech**: Spring AI + Gemini API

## Quan hệ giữa các vai trò

```
## Quan hệ giữa các vai trò

### 1. Cách các role được gán

| Role | Cách có được role |
|---|---|
| ADMIN | Do hệ thống cấp |
| STUDENT | Tự động khi TBM thêm sinh viên vào danh sách làm đồ án của kỳ |
| TEACHER | Tự động khi TBM thêm giáo viên vào danh sách giáo viên của kỳ |
| HEAD_OF_DEPARTMENT | Gán cho giáo viên giữ chức Trưởng bộ môn |
| ADVISOR, OUTLINE_REVIEWER | TBM phân công |
| COUNCIL_CHAIR, COUNCIL_SECRETARY, COUNCIL_MEMBER | Tự động khi TBM tạo hội đồng (1 Chủ tịch, 1 Thư ký, 3 Ủy viên) |
| OPPONENT | Chủ tịch hoặc Thư ký phân công, chọn 2 trong 5 thành viên hội đồng |

- Một tài khoản có thể có nhiều role (ví dụ vừa là ADVISOR vừa là COUNCIL_MEMBER).
- Admin có thể thu hồi role của tài khoản vi phạm chính sách của trường.

### 2. Quan hệ kế thừa

```
TEACHER (role nền: xem tài liệu mẫu, thông tin cá nhân, thông báo)
  ├── HEAD_OF_DEPARTMENT
  ├── ADVISOR
  ├── OUTLINE_REVIEWER
  ├── OPPONENT
  ├── COUNCIL_CHAIR
  ├── COUNCIL_SECRETARY  (có toàn bộ quyền của COUNCIL_CHAIR)
  └── COUNCIL_MEMBER
```

### 3. Chức năng chính của từng role

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

Supervisor (Giáo viên hướng dẫn)
  ├── Duyệt / từ chối đơn đăng ký hướng dẫn của sinh viên
  ├── Duyệt / từ chối tên đề tài
  ├── Xem sinh viên hướng dẫn và đề cương của họ
  ├── Xem, duyệt / từ chối báo cáo cuối
  ├── Nộp file kết quả kiểm tra trùng báo cáo
  ├── Nộp phiếu theo dõi tiến trình sinh viên
  └── Chấm điểm hướng dẫn

Commenting Lecturer (Giáo viên nhận xét)
  ├── Xem sinh viên nhận xét và đề cương của họ
  └── Duyệt / từ chối đề cương (từ chối bắt buộc ghi nhận xét)

Examiner (Giáo viên phản biện)  ← 2 trong 5 thành viên hội đồng, không phải GVHD của sinh viên đó
  ├── Xem sinh viên phản biện
  ├── Tạo lịch phản biện
  ├── Duyệt / từ chối sinh viên đủ điều kiện ra hội đồng (trước buổi bảo vệ)
  └── Chấm điểm phản biện

Chairperson (Chủ tịch hội đồng)  ← chỉ thao tác trên hội đồng được phân công
  ├── Xem thông tin hội đồng, danh sách sinh viên, kế hoạch bảo vệ
  ├── Lập thời gian bảo vệ cụ thể (ca bảo vệ từng sinh viên)
  ├── Đề xuất đổi lịch bảo vệ (TBM xác nhận)
  ├── Phân công giáo viên phản biện (2 trong 5 thành viên)
  ├── Chấm điểm hội đồng
  └── Xem điểm tổng (final) của 5 thành viên hội đồng

Secretary (Thư ký hội đồng)
  ├── Có toàn bộ quyền của Chủ tịch hội đồng
  └── Quản lý biên bản bảo vệ: thêm, xem, sửa, xóa

COUNCILMEMBER (Ủy viên hội đồng)
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

### 4. Ràng buộc nghiệp vụ

- GVHD được ngồi trong hội đồng nhưng không được làm GVPB của sinh viên mình hướng dẫn.
- Hội đồng gồm 5 thành viên: 1 Chủ tịch, 1 Thư ký, 3 Ủy viên. Cả 5 người đều được chấm điểm hội đồng.
- Đổi lịch bảo vệ: Chủ tịch hoặc Thư ký đề xuất, TBM xác nhận.
- Điểm thành phần chỉ người chấm xem được. Chủ tịch và Thư ký xem được điểm tổng của 5 thành viên. Sinh viên chỉ xem điểm tổng đồ án.
- GVHD, GVNX, GVPB chỉ thấy sinh viên mình phụ trách. Sinh viên chỉ thấy dữ liệu của chính mình.
- Chỉ TBM xem được file quyết định có chữ ký hiệu trưởng.
- Chỉ TBM được thêm, sửa, xóa tài liệu mẫu. Mọi giáo viên và sinh viên chỉ xem.
- Thông báo do hệ thống tự động gửi.
```

## File Storage
- Hệ thống dùng **Cloudinary** để lưu trữ file (đề cương, báo cáo, tài liệu)
- Config: `ConfigCloudinary.java`
