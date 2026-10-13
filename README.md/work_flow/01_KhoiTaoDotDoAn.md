# Khởi tạo đợt đồ án

## 1. Mục đích nghiệp vụ

Đợt đồ án được tạo nhằm xác định một chu kỳ thực hiện Đồ án tốt nghiệp trong một học kỳ của Khoa Công nghệ Thông tin - Trường Đại học Thủy Lợi.

Đợt đồ án là thực thể trung tâm của hệ thống, dùng để quản lý toàn bộ vòng đời đồ án từ khi sinh viên đăng ký giảng viên hướng dẫn cho đến khi hoàn thiện điểm và hồ sơ.

Tất cả dữ liệu nghiệp vụ đều gắn với một đợt đồ án, bao gồm:

- Sinh viên tham gia
- Giảng viên hướng dẫn
- Đề tài
- Đề cương
- Báo cáo
- Phản biện
- Kế hoạch bảo vệ
- Kết quả bảo vệ
- Điểm số và hồ sơ

---

## 2. Phạm vi hệ thống

### Phạm vi khoa

Hệ thống chỉ phục vụ cho:

- Khoa Công nghệ Thông tin
- Trường Đại học Thủy Lợi

Không thiết kế cho mô hình đa khoa.

### Phạm vi đợt đồ án

Một đợt đồ án:

- Áp dụng cho toàn bộ Khoa CNTT.
- Không giới hạn theo ngành.
- Không giới hạn theo chuyên ngành.
- Không giới hạn theo lớp.

### Bộ môn

Trong khoa tồn tại nhiều bộ môn.

Tuy nhiên:

- Đợt đồ án không được tạo riêng theo từng bộ môn.
- Đợt đồ án áp dụng cho toàn bộ khoa.

Đầu mỗi học kỳ:

- Trưởng bộ môn thực hiện import danh sách giảng viên vào hệ thống.
- Giảng viên thuộc các bộ môn khác nhau trong khoa.

---

## 3. Số lượng đợt đồ án

Mỗi năm học có 2 đợt:

- Học kỳ 1 (HK1)
- Học kỳ 2 (HK2)

Ví dụ:

- HK1 - 2026-2027
- HK2 - 2026-2027

Không tồn tại nhiều đợt đồ án diễn ra đồng thời.

---

## 4. Actor tham gia

### Quản trị viên hệ thống

Là actor tham gia giai đoạn khởi tạo đợt đồ án.

Các quyền chính:

- Tạo đợt đồ án.
- Cập nhật thông tin đợt đồ án.
- Thiết lập timeline các giai đoạn.
- Điều chỉnh timeline khi cần.
- Theo dõi trạng thái đợt đồ án.

---

## 5. Loại đồ án

Hiện tại hệ thống chỉ quản lý:

- Đồ án tốt nghiệp

Mặc dù hiện chỉ có một loại đồ án, trường dữ liệu loại đồ án vẫn tồn tại trong mô hình nghiệp vụ.

---

## 6. Thông tin của một đợt đồ án

### Thông tin chung

- Mã đợt
- Tên đợt
- Loại đồ án
- Học kỳ
- Năm học

### Thời gian tổng thể

- Ngày bắt đầu
- Ngày kết thúc

### Trạng thái

- Mở đăng ký
- Đang thực hiện
- Hoàn thành

---

## 7. Timeline các giai đoạn

Mỗi giai đoạn đều có:

- Ngày bắt đầu
- Ngày kết thúc

Danh sách giai đoạn:

### Giai đoạn 1

Đăng ký giáo viên hướng dẫn

### Giai đoạn 2

Phân công giáo viên hướng dẫn

Áp dụng cho các sinh viên không đăng ký giáo viên hướng dẫn.

### Giai đoạn 3

Đăng ký đề tài

### Giai đoạn 4

Phê duyệt đề cương

### Giai đoạn 5

Thực hiện đồ án

### Giai đoạn 6

Nộp báo cáo

### Giai đoạn 7

Lập kế hoạch bảo vệ

### Giai đoạn 8

Lập kế hoạch phản biện

### Giai đoạn 9

Bảo vệ đồ án

### Giai đoạn 10

Hoàn thiện điểm và hồ sơ

---

## 8. Quy tắc timeline

### Quy tắc 1

Mỗi giai đoạn phải có:

- Ngày bắt đầu
- Ngày kết thúc

### Quy tắc 2

Các giai đoạn không được phép chồng lấn thời gian.

Ví dụ không hợp lệ:

- Đăng ký đề tài: 01/03 - 15/03
- Phê duyệt đề cương: 10/03 - 25/03

### Quy tắc 3

Các giai đoạn phải diễn ra tuần tự.

### Quy tắc 4

Các giai đoạn phải nằm trong khoảng thời gian của đợt đồ án.

---

## 9. Điều kiện mở đợt

Không có điều kiện tiên quyết.

Quản trị viên có thể:

1. Tạo đợt đồ án.
2. Khai báo timeline.
3. Mở đợt.

Sau đó các nghiệp vụ khác mới bắt đầu.

Không yêu cầu phải có sẵn:

- Sinh viên
- Giảng viên
- Đề tài

trước khi tạo đợt.

---

## 10. Trạng thái đợt đồ án

### Mở đăng ký

Giai đoạn đầu của đợt đồ án.

Các hoạt động chính:

- Đăng ký GVHD
- Phân công GVHD
- Đăng ký đề tài
- Phê duyệt đề cương

### Đang thực hiện

Các hoạt động chính:

- Thực hiện đồ án
- Nộp báo cáo
- Lập kế hoạch phản biện
- Lập kế hoạch bảo vệ
- Bảo vệ đồ án
- Chấm điểm

### Hoàn thành

Kết thúc đợt đồ án.

Dữ liệu được lưu trữ phục vụ tra cứu và báo cáo.

---

## 11. Cơ chế chuyển trạng thái

Việc chuyển trạng thái được thực hiện tự động theo thời gian.

Không yêu cầu quản trị viên thao tác chuyển trạng thái thủ công.

Mô hình tổng quát:

Mở đăng ký
→ Đang thực hiện
→ Hoàn thành

---

## 12. Chỉnh sửa đợt đồ án

Cho phép chỉnh sửa timeline ngay cả khi:

- Đợt đã bắt đầu.
- Đang có người sử dụng hệ thống.

Thông tin hiện tại chưa quy định các giới hạn chi tiết khi chỉnh sửa các mốc đã diễn ra.

---

## 13. Hủy đợt đồ án

Không được phép hủy đợt đồ án.

Không được phép xóa đợt đồ án sau khi đã tạo.

---

## 14. Các ràng buộc nghiệp vụ đã xác định

### RB01

Mỗi năm học có đúng 2 đợt:

- HK1
- HK2

### RB02

Không tồn tại nhiều đợt đồ án đồng thời.

### RB03

Mỗi đợt phải có:

- Học kỳ
- Năm học

### RB04

Ngày bắt đầu phải nhỏ hơn ngày kết thúc.

### RB05

Mỗi giai đoạn phải có ngày bắt đầu và ngày kết thúc.

### RB06

Timeline các giai đoạn không được chồng lấn.

### RB07

Mọi giai đoạn phải nằm trong thời gian của đợt đồ án.

### RB08

Cặp giá trị (Học kỳ, Năm học) là duy nhất.

Ví dụ:

- HK1 - 2026-2027
- HK2 - 2026-2027

là hợp lệ.

Tồn tại hai đợt cùng:

- HK1 - 2026-2027

là không hợp lệ.

### RB09

Không được phép hủy đợt đồ án.

---

## 15. Nhận định phục vụ thiết kế hệ thống

Từ góc nhìn phân tích nghiệp vụ, hệ thống nhiều khả năng cần hai khái niệm riêng biệt:

### Trạng thái đợt đồ án

- Mở đăng ký
- Đang thực hiện
- Hoàn thành

### Giai đoạn hiện tại (Current Phase)

Được suy ra từ timeline.

Ví dụ:

Nếu ngày hiện tại nằm trong khoảng thời gian của giai đoạn "Đăng ký đề tài" thì:

Current Phase = Đăng ký đề tài

Các chức năng được mở hoặc khóa nên dựa chủ yếu vào Current Phase thay vì chỉ dựa vào trạng thái đợt đồ án.

Đây là nhận định thiết kế được suy ra từ các thông tin nghiệp vụ đã được cung cấp.
