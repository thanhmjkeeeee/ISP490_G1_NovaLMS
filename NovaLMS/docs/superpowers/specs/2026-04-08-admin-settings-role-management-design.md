# Design: Admin Settings — Role Display Name Management

## 1. Overview

Thêm tab **"Vai trò (Roles)"** vào trang Admin Settings (`/admin/settings`) để quản trị viên có thể chỉnh sửa **tên hiển thị** (display name) của các role hệ thống. Hệ thống sẽ đọc role từ `setting` table thay vì hardcode role ID.

## 2. Mục tiêu

- Admin có thể xem danh sách tất cả roles hiện có
- Admin có thể thay đổi display name của bất kỳ role nào
- Role code (value) luôn read-only, không cho sửa
- Không cho thêm mới hoặc xóa role — chỉ edit tên hiển thị
- Thay đổi tên hiển thị phản ánh ngay trên UI (sidebar, header...) sau khi lưu

## 3. Data Model

### Setting Entity (hiện có, không đổi schema)

```java
@Entity @Table(name = "setting")
public class Setting {
    Integer settingId;   // PK: 201, 202, 203, 204, 205
    String name;         // Display name: "Quản trị viên", "Giáo viên"...
    String value;        // Code: "ADMIN", "MANAGER", "EXPERT", "TEACHER", "STUDENT"
    String settingType; // = "ROLE"
    Integer orderIndex;
    String status;      // "Active" / "Inactive"
    String description;
}
```

### Seeded Data

| settingId | name (display) | value (code) | settingType |
|---|---|---|---|
| 201 | Quản trị viên | ADMIN | ROLE |
| 202 | Quản lý | MANAGER | ROLE |
| 203 | Chuyên gia | EXPERT | ROLE |
| 204 | Giáo viên | TEACHER | ROLE |
| 205 | Học sinh | STUDENT | ROLE |

> ⚠️ `DataInitializerAuth.java` hiện tại hardcode `ADMIN=201, MANAGER=202...`. Sau khi implement xong, code cần đọc `setting` table thay vì hardcode. Chi tiết ở section 6.

## 4. API Endpoints

Dùng lại `SettingApiController` hiện có, chỉ thêm endpoint cho roles:

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/settings?type=ROLE` | Lấy danh sách roles (hiện có) |
| `PUT` | `/api/settings/{id}` | Cập nhật display name (hiện có) |

## 5. Frontend Changes

### 5.1 Tab HTML (settings.html)

Thêm tab thứ 5 vào `settingTabs`:

```html
<li class="nav-item" role="presentation">
    <button class="nav-link border-0" id="role-tab" data-bs-toggle="tab"
            data-type="ROLE" type="button" role="tab">
        <i class="bi bi-people me-1"></i> Vai trò
    </button>
</li>
```

### 5.2 Table Columns cho Roles tab

| ID | Role Code | Tên hiển thị | Thao tác |
|---|---|---|---|
| 201 | ADMIN | Quản trị viên | ✏️ |
| 202 | MANAGER | Quản lý | ✏️ |
| 203 | EXPERT | Chuyên gia | ✏️ |
| 204 | TEACHER | Giáo viên | ✏️ |
| 205 | STUDENT | Học sinh | ✏️ |

### 5.3 Inline Edit Modal

Dùng lại modal `settingModal` hiện tại với adaptations:

- `settingType` dropdown ẩn hoặc disabled cho ROLE tab (role code không đổi)
- Chỉ cho sửa: `name` (display name), `description`
- `status` và `orderIndex` ẩn hoàn toàn với ROLE tab
- Modal title: "Chỉnh sửa vai trò"

### 5.4 JavaScript Changes

- `currentType` sẽ là `'ROLE'` khi user đang ở role tab
- Khi `currentType === 'ROLE'`: ẩn `settingType` dropdown + `statusGroup` trong modal
- Khi user click edit trên role row: set `settingType = 'ROLE'`, disable dropdown

## 6. Backend Changes

### 6.1 SettingService — thêm method tiện lợi

```java
public List<Setting> getRoles() {
    return settingRepository.findBySettingType("ROLE");
}
```

### 6.2 Authorization — Đọc role từ Setting table thay vì hardcode

**Files cần cập nhật:**

1. **`DataInitializerAuth.java`** — hiện tại hardcode role IDs:
   ```java
   // Cũ: hardcode
   roleAdmin.setRoleId(201);
   ```
   → Cần query `SettingRepository.findBySettingTypeAndValue("ROLE", "ADMIN")` để lấy đúng ID từ DB

2. **`SecurityConfig.java`** — kiểm tra role theo `setting_id`:
   - Hiện tại dùng `hasAuthority("ROLE_ADMIN")` với hardcoded role IDs
   - → Cần cập nhật để map đúng role ID từ `setting` table

3. **`User.java`** — `roleId` vẫn là Integer FK vào `setting` table, không đổi

> ⚠️ **Lưu ý:** Cần đảm bảo `DataInitializerAuth` chạy **sau** khi seed data mặc định để tránh race condition. Hoặc dùng `@DependsOn` / `@Order` annotation.

## 7. Security Considerations

- Chỉ **ADMIN** được phép truy cập `/admin/settings` (đã có trong SecurityConfig)
- Role code (`value`) không bao giờ được sửa qua API
- Validation: `name` không được rỗng, max 50 ký tự

## 8. Files to Modify

| File | Change Type |
|---|---|
| `templates/admin/settings.html` | Thêm tab Roles + JS adaptations |
| `service/SettingService.java` | Thêm `getRoles()` method |
| `configuration/DataInitializerAuth.java` | Đọc role từ Setting DB thay vì hardcode |
| `configuration/SecurityConfig.java` | Map role authority từ Setting DB |

## 9. Out of Scope (không làm trong spike này)

- Thêm / xóa role (CRUD đầy đủ)
- Permission/authority riêng
- CEFR, Question Type, Skill tab improvements (validation, ordering)
- Import/export settings
