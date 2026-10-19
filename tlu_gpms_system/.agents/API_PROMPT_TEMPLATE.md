# Mẫu Prompt Gửi Cho AI Agent Khi Viết API Mới

Dùng template này để gửi yêu cầu cho AI Agent. Bạn chỉ cần copy mẫu, điền các thông tin và dán vào chat.

> Lưu ý chung:
> - Role PHẢI lấy đúng tên trong `common/enums/Role.java`: `ADMIN`, `HEAD_OF_DEPARTMENT`, `SECRETARY`, `CHAIRPERSON`, `COUNCIL_MEMBER`, `SUPERVISOR`, `EXAMINER`, `COMMENTING_LECTURER`, `STUDENT`.
> - Phân quyền (`@PreAuthorize` / `@PostAuthorize`) luôn đặt ở **ServiceImpl**, không đặt ở Controller.
> - Endpoint dùng danh từ số ít theo code hiện có (`/student`, `/teacher`, `/topic`...).
> - Trạng thái nghiệp vụ nào chưa chốt thì ghi "chưa chốt" để Agent hỏi lại, không để Agent tự bịa.

---

## 1. Mẫu Nhanh (Dành cho CRUD cơ bản)

```markdown
### Feature: [Tên module, ví dụ: topic / outline / semester]
- Vị trí: com.example.tlu_gpms_system.features.[module]
- Chức năng: [Mô tả ngắn, ví dụ: Lấy danh sách đề tài theo học kỳ / Xóa đề tài]
- Endpoint: [GET / POST / PUT / DELETE] [URL, ví dụ: GET /topic?semesterId=...]
- Phân quyền:
  + Theo role: [Tên role trong Role.java, ví dụ: HEAD_OF_DEPARTMENT]
  + Hoặc theo permission: [Tên permission, ví dụ: TOPIC_VIEW — nhớ thêm vào DATA_ROLE_PERMISSION.txt]
  + Kiểm tra sở hữu (nếu có): [Ví dụ: SUPERVISOR chỉ thấy SV mình hướng dẫn]
- Request:
  + Param / Body: [Liệt kê các trường dữ liệu truyền lên + ràng buộc validate]
- Response mong muốn:
  + [Nếu GET: Liệt kê các trường cần hiển thị lên FE]
  + [Nếu POST/PUT/DELETE: Message tiếng Việt khi thành công]
- Xóa: [Xóa cứng / Xóa mềm (đổi status)]
- Ràng buộc: [Ví dụ: Chỉ lấy sinh viên có status trong account là true]
```

---

## 2. Mẫu Đầy Đủ (Dành cho API có nghiệp vụ phức tạp)

```markdown
### Tính năng: [Tên tính năng, ví dụ: Duyệt / Từ chối Đề cương]
1. Định danh & Phân quyền:
   - Module: [Ví dụ: features/outline]
   - Endpoint: [Ví dụ: PUT /outline/{publicId}/review]
   - Phân quyền (đặt ở ServiceImpl): [Ví dụ: hasRole('COMMENTING_LECTURER')]
   - Kiểm tra sở hữu: [Ví dụ: Người gọi phải là GVNX được phân công cho đề tài này -> nếu không: FORBIDDEN]
   - Lấy user hiện tại: [Có/Không — nếu có, dùng AuthenticationService.getAccountInfo() trong Service]

2. Dữ liệu đầu vào (Request):
   - PathVariable: [Ví dụ: publicId (UUID đề cương)]
   - Request Body:
     + status: [APPROVED / REVISION_REQUIRED] (bắt buộc)
     + feedback: [Nội dung nhận xét] (bắt buộc nếu REVISION_REQUIRED, tối đa 1000 ký tự)

3. Nghiệp vụ & Điều kiện kiểm tra (Business Logic):
   - Bước 1: Kiểm tra đề cương có tồn tại không -> Nếu không: ném OUTLINE_NOT_FOUND
   - Bước 2: Kiểm tra người gọi API có đúng là GVNX được phân công cho đề tài này không -> Nếu không: ném FORBIDDEN
   - Bước 3: Kiểm tra trạng thái hiện tại của đề cương (chỉ được duyệt khi đang ở trạng thái SUBMITTED) -> Nếu sai: ném INVALID_OUTLINE_STATUS
   - Bước 4: Cập nhật trạng thái đề cương và lưu feedback

4. Dữ liệu trả về (Response):
   - Trả về: APIResponse<Void>
   - Code: 1000
   - Message: "Đánh giá đề cương thành công"

5. Tác vụ phụ đi kèm (Side-effects):
   - Thông báo SSE: Gửi notification cho Sinh viên sở hữu đề cương ("Giáo viên nhận xét đã đánh giá đề cương của bạn")
   - File Cloudinary: [Nếu có upload/xóa file]
   - Trạng thái khác: [Nếu có đổi trạng thái đề tài]

6. Validation & ErrorCode:
   - Key validate mới cần thêm vào ErrorCode: [Ví dụ: FEEDBACK_EMPTY, FEEDBACK_TOO_LONG (4000, BAD_REQUEST)]
   - Lỗi nghiệp vụ mới: [Ví dụ: OUTLINE_NOT_FOUND (3xxx, NOT_FOUND), INVALID_OUTLINE_STATUS (3xxx, BAD_REQUEST)]

7. Test cần viết:
   - Unit test ServiceImpl: [Ví dụ: review_success, review_outlineNotFound_throwException, review_notAssigned_throwForbidden, review_wrongStatus_throwException]
   - Integration test (nếu cần kiểm tra phân quyền): [Ví dụ: STUDENT gọi -> 403, code 2001]

8. Tài liệu:
   - [Có/Không] cập nhật readme.md/case_execution/<tinh_nang>.md
```

---

## 3. Ví dụ Thực Tế

> Các trạng thái đề tài / đề cương trong ví dụ dưới đây **chưa chốt**. Khi viết code thật, Agent phải hỏi lại user danh sách enum trạng thái trước.

### Ví dụ 1: API Đăng ký đề tài (Sinh viên đăng ký)
```markdown
### Tính năng: Sinh viên đăng ký đề tài đồ án
1. Định danh & Phân quyền:
   - Module: features/topic
   - Endpoint: POST /topic/{publicId}/register
   - Phân quyền (ServiceImpl): hasRole('STUDENT')
   - Lấy user hiện tại: Có (getAccountInfo() để lấy Student của SV đăng nhập)

2. Dữ liệu đầu vào (Request):
   - PathVariable: publicId (UUID của đề tài)
   - Body: Không (chỉ cần publicId đề tài)

3. Nghiệp vụ (Business Logic):
   - Đề tài phải tồn tại và đang ở trạng thái APPROVED (chưa chốt)
   - Sinh viên chưa đăng ký đề tài nào trong học kỳ hiện tại (mỗi SV chỉ được 1 đề tài)
   - Đề tài chưa có sinh viên khác đăng ký (hoặc chưa đủ số lượng SV tối đa)
   - Gán sinh viên vào đề tài, chuyển trạng thái đăng ký sang "chờ GVHD (SUPERVISOR) duyệt" (tên enum chưa chốt)

4. Dữ liệu trả về:
   - APIResponse<Void>, code 1000, message: "Đăng ký đề tài thành công, vui lòng chờ GVHD phê duyệt"

5. Tác vụ phụ (Side-effects):
   - Gửi thông báo SSE cho GVHD của đề tài: "Sinh viên [Tên SV] vừa đăng ký đề tài của bạn"

6. Validation & ErrorCode:
   - TOPIC_NOT_FOUND, TOPIC_NOT_AVAILABLE, STUDENT_ALREADY_REGISTERED (3xxx)

7. Test cần viết:
   - registerTopic_success, registerTopic_topicNotFound_throwException, registerTopic_alreadyRegistered_throwException
```

### Ví dụ 2: API Lấy danh sách đề tài phân trang (Cho Sinh viên tra cứu)
```markdown
### Tính năng: Lấy danh sách đề tài cho sinh viên đăng ký
1. Định danh & Phân quyền:
   - Module: features/topic
   - Endpoint: GET /topic
   - Phân quyền (ServiceImpl): hasAnyRole('STUDENT', 'SUPERVISOR', 'HEAD_OF_DEPARTMENT')

2. Dữ liệu đầu vào:
   - Param: semesterId (UUID, tùy chọn), keyword (tìm theo tên, tùy chọn), page, size, sort
   - Controller dùng @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)

3. Nghiệp vụ:
   - Chỉ lấy các đề tài có trạng thái APPROVED (chưa chốt)
   - Lọc theo học kỳ nếu có truyền, tìm kiếm theo tên đề tài
   - Nhiều field + filter tùy chọn -> dùng DTO Projection + countQuery (theo mẫu StudentRepository.findAllStudents)

4. Dữ liệu trả về:
   - APIResponse<Page<TopicResponse>>, code 1000
   - DTO Response gồm: publicId, title, description, maxStudents, currentStudents, supervisorName, majorName
```
