# Mẫu Prompt Gửi Cho AI Agent Khi Viết API Mới

Dùng template này để gửi yêu cầu cho AI Agent. Bạn chỉ cần copy mẫu, điền các thông tin và dán vào chat.

---

## 1. Mẫu Nhanh (Dành cho CRUD cơ bản)

```markdown
### Feature: [Tên module, ví dụ: topic / outline / semester]
- Vị trí: com.example.tlu_gpms_system.features.[module]
- Chức năng: [Mô tả ngắn, ví dụ: Lấy danh sách đề tài theo học kỳ / Xóa đề tài]
- Endpoint: [GET / POST / PUT / DELETE] [URL, ví dụ: GET /topics?semesterId=...]
- Phân quyền theo [role hay permission]: 
- Request:
  + Param / Body: [Liệt kê các trường dữ liệu truyền lên]
- Response mong muốn:
  + [Nếu GET: Liệt kê các trường cần hiển thị lên FE]
  + [Nếu POST/PUT/DELETE: Message tiếng Việt khi thành công]
- Ràng buộc: [Ví dụ: Chỉ lấy sinh viên có status trong account là true]
```

---

## 2. Mẫu Đầy Đủ (Dành cho API có nghiệp vụ phức tạp)

```markdown
### Tính năng: [Tên tính năng, ví dụ: Duyệt / Từ chối Đề cương]
1. Định danh & Phân quyền:
   - Module: [Ví dụ: features/outline]
   - Endpoint: [Ví dụ: PUT /outlines/{publicId}/review]
   - Phân quyền: [Ví dụ: TEACHER / COMMENTING_LECTURER]
   - Lấy user hiện tại: [Có cần lấy thông tin user đăng nhập để check quyền không? (Có/Không)]

2. Dữ liệu đầu vào (Request):
   - PathVariable: [Ví dụ: publicId (UUID đề cương)]
   - Request Body:
     + status: [APPROVED / REJECTED] (bắt buộc)
     + feedback: [Nội dung nhận xét] (bắt buộc nếu REJECTED, tối đa 1000 ký tự)

3. Nghiệp vụ & Điều kiện kiểm tra (Business Logic):
   - Bước 1: Kiểm tra đề cương có tồn tại không -> Nếu không: ném OUTLINE_NOT_FOUND
   - Bước 2: Kiểm tra người gọi API có đúng là Giảng viên nhận xét được phân công cho đề tài này không -> Nếu không: ném FORBIDDEN
   - Bước 3: Kiểm tra trạng thái hiện tại của đề cương (chỉ được duyệt khi đang ở trạng thái SUBMITTED) -> Nếu sai: ném INVALID_OUTLINE_STATUS
   - Bước 4: Cập nhật trạng thái đề cương và lưu feedback

4. Dữ liệu trả về (Response):
   - Trả về: APIResponse<Void>
   - Code: 1000
   - Message: "Đánh giá đề cương thành công"

5. Tác vụ phụ đi kèm (Side-effects):
   - Thông báo SSE: Gửi notification cho Sinh viên sở hữu đề cương ("Giáo viên nhận xét đã đánh giá đề cương của bạn")
   - File Cloudinary: [Nếu có upload/xóa file]
   - Audit / Trạng thái khác: [Nếu có đổi trạng thái đề tài sang IN_PROGRESS]
```

---

## 3. Ví dụ Thực Tế

### Ví dụ 1: API Đăng ký đề tài (Sinh viên đăng ký)
```markdown
### Tính năng: Sinh viên đăng ký đề tài đồ án
1. Định danh & Phân quyền:
   - Module: features/topic
   - Endpoint: POST /topics/{publicId}/register
   - Phân quyền: STUDENT
   - Lấy user hiện tại: Có (Lấy account.id của SV đăng nhập)

2. Dữ liệu đầu vào (Request):
   - PathVariable: publicId (UUID của đề tài)
   - Body: Không (chỉ cần publicId đề tài)

3. Nghiệp vụ (Business Logic):
   - Đề tài phải tồn tại và đang ở trạng thái APPROVED
   - Sinh viên chưa đăng ký đề tài nào trong học kỳ hiện tại (mỗi SV chỉ được 1 đề tài)
   - Đề tài chưa có sinh viên khác đăng ký (hoặc chưa đủ số lượng SV tối đa)
   - Gán sinh viên vào đề tài, chuyển trạng thái đề tài sang PENDING_ADVISOR_APPROVAL

4. Dữ liệu trả về:
   - APIResponse<Void>, code 1000, message: "Đăng ký đề tài thành công, vui lòng chờ GVHD phê duyệt"

5. Tác vụ phụ (Side-effects):
   - Gửi thông báo SSE cho GVHD của đề tài: "Sinh viên [Tên SV] vừa đăng ký đề tài của bạn"
```

### Ví dụ 2: API Lấy danh sách đề tài phân trang (Cho Sinh viên tra cứu)
```markdown
### Tính năng: Lấy danh sách đề tài cho sinh viên đăng ký
1. Định danh & Phân quyền:
   - Module: features/topic
   - Endpoint: GET /topics
   - Phân quyền: STUDENT, TEACHER
   
2. Dữ liệu đầu vào:
   - Param: semesterId (UUID, tùy chọn), keyword (tìm theo tên, tùy chọn), page, size, sort

3. Nghiệp vụ:
   - Chỉ lấy các đề tài có trạng thái APPROVED
   - Lọc theo học kỳ nếu có truyền, tìm kiếm theo tên đề tài

4. Dữ liệu trả về:
   - APIResponse<Page<TopicResponse>>, code 1000
   - DTO Response gồm: publicId, title, description, maxStudents, currentStudents, supervisorName, departmentName
```
