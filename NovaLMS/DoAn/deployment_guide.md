# 🚀 Hướng dẫn Deployment - Nova LMS trên Render

Tài liệu này hướng dẫn bạn cách triển khai code Spring Boot và Database MySQL lên nền tảng **Render**.

## 1. Chuẩn bị Database (MySQL)

Bạn có thể chọn 1 trong 2 cách sau:

### Cách A: Dùng Railway.app (Nhanh & Tiện)
1.  Truy cập [railway.app](https://railway.app/) và đăng nhập bằng GitHub.
2.  Nhấn **New Project** -> **Provision MySQL**.
3.  Sau khi tạo xong, nhấn vào thẻ **MySQL** -> chọn **Variables**.
4.  Copy các thông tin sau để dùng cho Render:
    *   `MYSQL_URL`: Đây là chuỗi kết nối (dạng `mysql://user:pass@host:port/db`).
    *   Hoặc lấy lẻ: `MYSQLHOST`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLPORT`, `MYSQLDATABASE`.

### Cách B: Dùng Aiven.io (Miễn phí bền vững)
1.  Truy cập [aiven.io](https://aiven.io/) và tạo tài khoản.
2.  Tạo mới một service **MySQL** (chọn gói **Free**).
3.  Khi service ở trạng thái "Running", hãy copy:
    *   **Service URI** (Dạng `mysql://avnadmin:password@host:port/defaultdb`).
    *   Hoặc các thông số: Host, Port, User, Password.

---

## 2. Chuẩn bị Data
Bạn có 2 cách để đưa dữ liệu từ file `massive_seed_v2.sql` vào database:

### Cách 1: Dùng công cụ (DBeaver / MySQL Workbench)
1.  Kết nối vào database Railway bằng các thông số `MYSQLHOST`, `MYSQLPORT`...
2.  Mở và chạy file [massive_seed_v2.sql](file:///c:/Users/huy/Desktop/New%20folder/ISP490_G1_NovaLMS/NovaLMS/DoAn/massive_seed_v2.sql).

### Cách 2: Dùng API (Tự động - Khuyên dùng)
Sau khi bạn đã deploy thành công ứng dụng lên Render và App đã ở trạng thái **Live**, bạn chỉ cần gọi một lệnh API để hệ thống tự đọc file SQL và đổ vào database:
1.  Dùng Postman hoặc trình duyệt, gửi request **POST** tới URL:
    `https://<ten-app-cua-ban>.onrender.com/api/v1/admin/debug/seed-v2`
2.  Nếu nhận được thông báo "Database seeded successfully", nghĩa là toàn bộ dữ liệu đã được nạp xong.

> [!NOTE]
> Tôi đã comment sẵn các dòng `CREATE DATABASE` trong file SQL để đảm bảo nó chạy được trên mọi môi trường Cloud.

## 3. Triển khai Code lên Render

1.  **Đưa code lên GitHub**: Đảm bảo toàn bộ project (bao gồm cả `Dockerfile` và `render.yaml`) đã được push lên một repository cá nhân trên GitHub.
2.  **Tạo Web Service trên Render**:
    -   Đăng nhập [Render.com](https://render.com/).
    -   Nhấn **New +** -> **Blueprint**.
    -   Kết nối repository GitHub của bạn.
    -   Render sẽ tự động đọc file `render.yaml` và liệt kê các Environment Variables cần thiết.

## 4. Cấu hình Environment Variables (Biến môi trường)
Trên bảng điều khiển Render, hãy điền các giá trị sau:

| Biến môi trường | Giá trị (Tìm trên Railway/Aiven/Google/Cloudinary) |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | **Dạng JDBC**: `jdbc:mysql://<host>:<port>/<dbname>` (Lưu ý Railway chỉ có `mysql://`, bạn phải đổi thành `jdbc:mysql://`) |
| `SPRING_DATASOURCE_USERNAME` | Tên User (thường là `root` trên Railway hoặc `avnadmin` trên Aiven) |
| `SPRING_DATASOURCE_PASSWORD` | Mật khẩu Database |
| `APP_URL` | URL của App sau khi Render tạo xong (vd: `https://my-app.onrender.com`) |
| `GOOGLE_CLIENT_ID` | Lấy từ Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | Lấy từ Google Cloud Console |
| `CLOUDINARY_CLOUD_NAME` | *(Tên cloud từ Cloudinary)* |
| `CLOUDINARY_API_KEY` | *(API Key từ Cloudinary)* |
| `CLOUDINARY_API_SECRET` | *(API Secret từ Cloudinary)* |
| `MAIL_USERNAME` | *(Email gửi thông báo - gmail)* |
| `MAIL_PASSWORD` | *(App Password của Gmail)* |
| `GROQ_API_KEY` | *(Key từ Groq.com)* |
| `PAYOS_CLIENT_ID` | *(Client ID từ PayOS)* |
| `PAYOS_API_KEY` | *(API Key từ PayOS)* |
| `PAYOS_CHECKSUM_KEY` | *(Checksum Key từ PayOS)* |

> [!IMPORTANT]
> Sau khi deploy thành công, bạn phải vào **Google Cloud Console**, mục **Credentials**, và thêm `https://your-app.onrender.com/login/oauth2/code/google` vào danh sách **Authorized redirect URIs**.

## 5. Kiểm tra
Sau khi Render báo **"Your service is live"**, hãy truy cập vào `APP_URL` và đăng nhập thử với các tài khoản đã seed:
-   **Admin**: `admin@novalms.com` / `123456`
-   **Manager**: `manager@novalms.com` / `123456`
-   **Teacher**: `teacher1@novalms.com` / `123456`
-   **Student**: `student1@novalms.com` / `123456`
