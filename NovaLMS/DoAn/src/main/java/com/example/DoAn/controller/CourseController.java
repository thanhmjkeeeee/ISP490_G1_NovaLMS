package com.example.DoAn.controller;

import com.example.DoAn.service.CourseService;
import com.example.DoAn.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.DoAn.service.SettingService;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private SettingService settingService;

    @GetMapping({"/courses"})
    public String listCourses(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            Model model) {

        List<Course> list;
        if (categoryId != null) {
            // Nếu có chọn category, gọi hàm lọc
            list = courseService.getCoursesByCategory(categoryId);
        } else {
            // Nếu không, lấy tất cả khóa học 'Published' (hoặc 'Active' tùy bạn chọn)
            list = courseService.getAllPublishedCourses();
        }

        model.addAttribute("courseList", list);
        model.addAttribute("categories", settingService.getCourseCategories());
        model.addAttribute("selectedCat", categoryId); // Để giữ trạng thái đã chọn trên giao diện

        return "public/courses";
    }

    @GetMapping("/course/details/{id}")
    public String viewCourseDetails(@PathVariable("id") Integer id, Model model) {
        // 1. Lấy thông tin cơ bản của khóa học
        Course course = courseService.getCourseById(id);
        if (course == null) {
            return "redirect:/courses";
        }

        // 2. Lấy danh sách lớp đang mở - Dùng hàm bạn vừa thêm ở Service
        // Lưu ý: Tên biến 'activeClasses' phải khớp với th:each trong HTML của bạn
        model.addAttribute("activeClasses", courseService.getActiveClassesByCourse(id));

        // 3. Lấy số lượng học viên - Dùng hàm bạn vừa thêm ở Service
        // Lưu ý: Tên biến 'studentCount' phải khớp với ${studentCount} trong HTML
        model.addAttribute("studentCount", courseService.getStudentCount(id));

        // 4. Đưa thông tin khóa học sang HTML
        model.addAttribute("course", course);
        model.addAttribute("curriculum", courseService.getCourseCurriculum(id));
        return "public/course-details";
    }

    @GetMapping("/courses/filter")
    public String filterCourses(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "sortBy", required = false, defaultValue = "newest") String sortBy,
            Model model) {

        // 1. Gọi Service xử lý logic tìm kiếm & sắp xếp
        List<Course> list = courseService.searchAndFilterCourses(keyword, categoryId, sortBy);

        // 2. Đẩy dữ liệu vào Model
        model.addAttribute("courseList", list);

        // 3. Trả về Fragment (Phần HTML danh sách khóa học trong file courses.html)
        // Lưu ý: "courses" là tên file html, "courseListFragment" là tên th:fragment
        return "public/courses :: courseListFragment";
    }

}