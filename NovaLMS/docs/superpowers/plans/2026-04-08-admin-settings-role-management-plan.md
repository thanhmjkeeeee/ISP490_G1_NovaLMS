# Admin Settings Role Management — Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan.

**Goal:** Thêm tab Roles vào trang Admin Settings để admin chỉnh sửa display name của các role hệ thống, đồng thời cập nhật `DataInitializerAuth` đọc role từ `setting` table thay vì hardcode.

**Architecture:**
- Frontend: Thêm tab thứ 5 "Vai trò" vào `admin/settings.html` — dùng lại modal edit hiện có, ẩn các trường không liên quan khi tab = ROLE
- Backend: Thêm `getRoles()` vào `SettingService`, cập nhật `DataInitializerAuth` query role từ DB thay vì hardcode ID 201/202/203/204/205
- API: Dùng lại `/api/settings` CRUD hiện có

**Tech Stack:** Spring Boot 3, Thymeleaf, jQuery DataTables, Bootstrap 5

---

## Chunk 1: Frontend — Thêm tab Roles + JS adaptations

**Files:**
- Modify: `DoAn/src/main/resources/templates/admin/settings.html`

### Steps

- [ ] **Step 1: Thêm tab "Vai trò" vào nav-tabs**

Tìm đoạn `settingTabs`, thêm tab thứ 5:

```html
<li class="nav-item" role="presentation">
    <button class="nav-link border-0" id="role-tab" data-bs-toggle="tab"
            data-type="ROLE" type="button" role="tab">
        <i class="bi bi-people me-1"></i> Vai trò
    </button>
</li>
```

Thêm vào sau tab `skill-tab`.

- [ ] **Step 2: Cập nhật JS — ẩn modal fields khi tab = ROLE**

Tìm hàm `openSettingModal()`, thêm logic sau vào cuối function:

```javascript
// Ẩn/hiện modal fields theo tab
if (currentType === 'ROLE') {
    document.getElementById('settingTypeGroup').style.display = 'none';
    document.getElementById('statusGroup').style.display = 'none';
    document.getElementById('settingModalLabel').innerText = 'Chỉnh sửa vai trò';
} else {
    document.getElementById('settingTypeGroup').style.display = 'block';
    document.getElementById('statusGroup').style.display = 'block';
    document.getElementById('settingModalLabel').innerText = isEditing ? 'Chỉnh sửa Cấu hình' : 'Thêm Cấu hình mới';
}
```

- [ ] **Step 3: Thêm `settingTypeGroup` wrapper trong modal form**

Trong modal form, bọc `settingType` select bằng `div` có id:

```html
<div class="mb-3" id="settingTypeGroup">
    <label class="form-label">Loại cấu hình <span class="text-danger">*</span></label>
    <select class="form-select" id="settingType" name="settingType" required>
        ...
    </select>
</div>
```

- [ ] **Step 4: Cập nhật `saveSetting()` — không gửi `status` khi tab = ROLE**

Trong hàm `saveSetting()`, sửa object `data`:

```javascript
const data = {
    settingType: currentType, // luôn dùng current tab, không lấy từ dropdown
    name: nameVal,
    value: currentType === 'ROLE' ? row.value : nameVal.trim().toUpperCase().replace(/\s+/g, '_'),
    orderIndex: 0,
    status: currentType === 'ROLE' ? undefined : (isEditing ? $('#status').val() : 'Active'),
    description: $('#description').val()
};
```

- [ ] **Step 5: Cập nhật `editSetting()` — giữ đúng role khi edit**

Sửa `editSetting(row)` để KHÔNG ghi đè `settingType` khi đang ở role tab:

```javascript
function editSetting(row) {
    document.getElementById('settingId').value = row.settingId;
    document.getElementById('name').value = row.name;
    document.getElementById('description').value = row.description || '';

    document.getElementById('settingModalLabel').innerText = 'Chỉnh sửa vai trò';
    document.getElementById('settingTypeGroup').style.display = 'none';
    document.getElementById('statusGroup').style.display = 'none';
    myModal.show();
}
```

- [ ] **Step 6: Cập nhật DataTable columns cho role tab**

Tìm `table = $('#settingsTable').DataTable({...})`, thêm `createdRow` callback để format khác cho ROLE tab:

```javascript
createdRow: function(row, data, dataIndex) {
    if (data.settingType === 'ROLE') {
        $('td', row).eq(1).html('<span class="badge bg-secondary me-1">' +
            (data.value || '') + '</span> ' +
            '<strong>' + data.name + '</strong>');
    }
}
```

Cột 1 (index 1) sẽ hiển thị: `[ADMIN] Quản trị viên` thay vì chỉ tên.

---

## Chunk 2: Backend — SettingService + DataInitializerAuth

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/service/SettingService.java`
- Modify: `DoAn/src/main/java/com/example/DoAn/configuration/DataInitializerAuth.java`
- Modify: `DoAn/src/main/java/com/example/DoAn/configuration/SecurityConfig.java`

### Steps

- [ ] **Step 1: Thêm `getRoles()` method vào SettingService**

Mở `SettingService.java`, thêm method:

```java
public List<Setting> getRoles() {
    return settingRepository.findBySettingType("ROLE");
}
```

- [ ] **Step 2: Đọc DataInitializerAuth hiện tại**

Tìm file `DataInitializerAuth.java` trong `configuration/`. Đọc toàn bộ để hiểu cách nó hiện tại khởi tạo role (hardcode `roleId = 201` etc.), rồi thay bằng query từ `SettingRepository`.

Pattern cần tìm và thay (ước lượng — cần đọc file thực tế để xác định chính xác):

```java
// Cũ: Role admin = Role.builder().roleId(201).roleName("ADMIN")...
// Mới:
Setting adminRole = settingRepository.findBySettingTypeAndValue("ROLE", "ADMIN")
    .orElseThrow(() -> new RuntimeException("Role ADMIN not found in setting table"));
Role admin = Role.builder().roleId(adminRole.getSettingId()).roleName("ADMIN")...
```

Thực hiện tương tự cho MANAGER, EXPERT, TEACHER, STUDENT.

- [ ] **Step 3: Kiểm tra SecurityConfig**

Đọc `SecurityConfig.java`. Xác định xem có dùng hardcoded role IDs ở đâu không (VD: `.antMatchers("/admin/**").hasAuthority("ROLE_ADMIN")`). Nếu có, xem cách `CustomUserDetailsService` load authorities — nếu đã dùng `setting` table thì không cần sửa. Nếu hardcode, cập nhật để map đúng.

**Lưu ý:** Cần đọc file thực tế để xác định chính xác chỗ nào cần sửa.

- [ ] **Step 4: Verify build compiles**

```bash
cd NovaLMS/DoAn && ./mvnw compile -q
```

Expected: BUILD SUCCESS — không có compile error.

---

## Chunk 3: Test & Verify

**Files:**
- Không tạo file mới

### Steps

- [ ] **Step 1: Khởi chạy app**

```bash
cd NovaLMS/DoAn && ./mvnw spring-boot:run
```

Expected: App khởi động không lỗi, không crash vì role query.

- [ ] **Step 2: Login as admin, vào /admin/settings, click tab "Vai trò"**

Verify:
- Tab hiển thị đúng icon + text
- Table hiển thị 5 rows: ADMIN, MANAGER, EXPERT, TEACHER, STUDENT
- Cột Role Code hiển thị badge (ADMIN, MANAGER...)
- Cột Tên hiển thị đúng tên (Quản trị viên, Quản lý...)

- [ ] **Step 3: Click edit icon → verify modal**

Verify:
- Modal title: "Chỉnh sửa vai trò"
- Dropdown "Loại cấu hình" bị ẩn
- Dropdown "Trạng thái" bị ẩn
- Chỉ hiển thị: Tên hiển thị, Mô tả

- [ ] **Step 4: Đổi tên → Lưu → Verify cập nhật thành công**

VD: Đổi "Quản trị viên" → "Super Admin", save. Verify toastr success + table update.

- [ ] **Step 5: Verify API /api/settings?type=ROLE**

```bash
curl -s http://localhost:8080/api/settings?type=ROLE | jq .
```

Expected: JSON array 5 roles với đúng `settingId`, `value`, `name` đã sửa.
