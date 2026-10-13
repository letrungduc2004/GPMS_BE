# Project_Context.md

# Bối cảnh tổng thể đồ án

## Tên đề tài

**Hệ thống hỗ trợ quản lý quy trình thực hiện đồ án tốt nghiệp tại Khoa Công nghệ Thông tin, Trường Đại học Thủy lợi**

---

# 1. Mục tiêu của hệ thống
Hệ thống được xây dựng nhằm số hóa và tập trung hóa toàn bộ quy trình quản lý đồ án tốt nghiệp của Khoa Công nghệ Thông tin, Trường Đại học Thủy lợi.
Mục tiêu chính:
- Thay thế các công cụ quản lý rời rạc đang sử dụng:
  - Email
  - Google Sheets
  - Tài liệu thủ công
- Chuẩn hóa quy trình quản lý đồ án.
- Giảm thao tác thủ công.
- Theo dõi tiến độ đồ án tập trung.
- Hỗ trợ phân công và chấm điểm minh bạch.
- Đảm bảo tính liêm chính học thuật.
- Hỗ trợ thống kê và báo cáo.

---

# 2. Phạm vi hệ thống
## Trong phạm vi
- Website quản lý đồ án tốt nghiệp.
- Quản lý toàn bộ vòng đời đồ án.
- Quản lý người dùng và phân quyền.
- Quản lý hồ sơ đồ án.
- Quản lý đề cương.
- Quản lý báo cáo.
- Quản lý phản biện.
- Quản lý hội đồng.
- Quản lý chấm điểm.
- Quản lý lịch bảo vệ.
- Thống kê và báo cáo.

## Ngoài phạm vi
- Mobile Application.
- Chuyển đổi sang kiến trúc Microservice.

---

# 3. Đối tượng sử dụng
## Sinh viên
Thực hiện đồ án và nộp hồ sơ.

## Giáo viên hướng dẫn (GVHD)
Theo dõi và hướng dẫn sinh viên.

## Giáo viên nhận xét (GVNX)
Đánh giá đề cương.

## Giáo viên phản biện (GVPB)
Đánh giá phản biện đồ án.

## Chủ tịch hội đồng
Điều phối bảo vệ.

## Thư ký hội đồng
Ghi biên bản và hỗ trợ hội đồng.

## Trưởng bộ môn (TBM)
Quản lý toàn bộ quy trình đồ án.

## Admin
Quản trị hệ thống.

---
# 4. Kiến trúc hệ thống

## Kiến trúc
Monolithic Architecture

Lý do:
- Phù hợp quy mô đồ án tốt nghiệp.
- Dễ phát triển.
- Dễ triển khai.
- Dễ bảo trì.
- Không có yêu cầu Microservice.

---

# 5. Công nghệ sử dụng
## Frontend
- React

## Backend
- Spring Boot

## Database
- MySQL

## Hạ tầng
- Docker
- AWS

---

# 6. Yêu cầu phi chức năng

## Hiệu năng
- Hệ thống cần đáp ứng khoảng 200–300 người dùng đồng thời trong giai đoạn cao điểm.

## Bảo mật
- Áp dụng phân quyền RBAC.
- Bảo vệ dữ liệu người dùng.
- Mật khẩu phải được hash (không lưu plaintext).
- Chỉ người có thẩm quyền mới truy cập được dữ liệu tương ứng.

## Khả năng mở rộng
- Cho phép mở rộng chức năng trong tương lai.
- Không định hướng chuyển sang Microservice.

## Nền tảng

- Website.

---

# 7. Các vai trò trong hệ thống

1. Student
2. Admin
3. Head Of Department (TBM)
4. Supervisor (GVHD)
5. Reviewer (GVNX)
6. Examiner (GVPB)
7. Chairman
8. Secretary

Ghi chú:
Một giảng viên có thể đảm nhận nhiều vai trò khác nhau tùy học kỳ.
Ví dụ:
- GVHD
- GVPB
- Chủ tịch hội đồng
đồng thời trong cùng một học kỳ.
(Cần xác nhận chi tiết khi thiết kế RBAC.)

---

# 8. Các quy tắc nghiệp vụ đã xác nhận

## BR-01
Một sinh viên chỉ thực hiện một đồ án.
1 Sinh viên ↔ 1 Đồ án
---

## BR-02
Một sinh viên chỉ đăng ký một giáo viên hướng dẫn.
1 Sinh viên → 1 GVHD

---

## BR-03
Một đề tài thông thường có một GVHD.
1 Đề tài → 1 GVHD

---

## BR-04
Một số trường hợp đặc biệt có hai GVHD.
1 Đề tài → 2 GVHD

Ví dụ:
- GVHD chính
- GVHD phụ

---

## BR-05
Giáo viên nhận xét phải khác giáo viên hướng dẫn.
GVNX ≠ GVHD

---

## BR-06
Giáo viên phản biện phải khác giáo viên hướng dẫn.
GVPB ≠ GVHD

---

## BR-07
Điểm cuối cùng của sinh viên được tính bằng trung bình cộng điểm của 5 thành viên hội đồng.

---

## BR-08
Thông báo được gửi tự động theo sự kiện phát sinh.
Event-driven Notification

---

## BR-09
TBM là người tạo đơn đổi lịch.

---

## BR-10
Chủ tịch hoặc Thư ký có quyền xử lý đổi lịch bảo vệ.

---

# 9. Các module nghiệp vụ đã xác định

## Authentication

- Đăng nhập
- Đăng xuất

## User Management
- Quản lý tài khoản
- Phân quyền
- Trạng thái tài khoản

## Semester Management
- Quản lý học kỳ

## Student Management
- Quản lý sinh viên

## Lecturer Management
- Quản lý giảng viên

## Registration Management
- Đăng ký GVHD
- Đăng ký đề tài

## Thesis Management
- Đề tài
- Đề cương
- Báo cáo

## Review Management
- Nhận xét
- Phản biện

## Progress Management
- Theo dõi tiến độ
- Phiếu theo dõi

## Defense Management
- Hội đồng
- Lịch bảo vệ
- Đổi lịch

## Scoring Management
- Điểm hướng dẫn
- Điểm phản biện
- Điểm hội đồng

## Document Management
- Tài liệu mẫu

## Notification Management
- Thông báo

## Statistics Management
- Thống kê
- Báo cáo

---

# 10. Workflow tổng thể đã hiểu
Giai đoạn 1
Đăng ký GVHD
Sinh viên
→ Đăng ký GVHD
→ Đăng ký đề tài
Trương bộ môn
→ Phân công giáo viên hướng dẫn cho sinh viên (trường hợp chưa có giáo viên hướng dẫn)

GVHD
→ Duyệt/Từ chối

---

Giai đoạn 2
Đề cương
Sinh viên
→ Nộp đề cương

GVNX
→ Nhận xét
→ Duyệt/Từ chối

---

Giai đoạn 3
Thực hiện đồ án

GVHD
→ Theo dõi tiến độ
→ Nộp phiếu theo dõi
→ Chấm điểm tiến trình

---

Giai đoạn 4
Báo cáo
Sinh viên
→ Nộp báo cáo

GVHD
→ Kiểm tra
→ Check trùng lặp
→ Duyệt/Từ chối

---

Giai đoạn 5
Phản biện

GVPB
→ Đánh giá
→ Chấm điểm phản biện

---

Giai đoạn 6
Bảo vệ

TBM
→ Lập hội đồng
→ Phân công

Chủ tịch
→ Lập lịch

Thư ký
→ Ghi biên bản

Thành viên hội đồng
→ Chấm điểm

---
Giai đoạn 7
Kết thúc

TBM
→ Thống kê
→ Báo cáo

---

# 11. Trạng thái hiện tại của dự án

Hiện tại nhóm đang:
- Thiết kế hệ thống.
- Thiết kế ERD.

Đây là giai đoạn quan trọng vì ảnh hưởng trực tiếp tới:
- Database.
- API.
- Service.
- Phân quyền.
- Workflow.

---

# 12. Những thông tin chưa được cung cấp
Các nội dung sau chưa được mô tả chi tiết:
- Quy trình chi tiết của từng giai đoạn.
- Biểu mẫu thực tế.
- Quy định chi tiết về điểm số.
- State machine của từng workflow.
- Ràng buộc thời gian theo học kỳ.
- Quy trình vận hành thực tế của từng vai trò.
Các nội dung trên sẽ được bổ sung trong các cuộc trao đổi tiếp theo.
