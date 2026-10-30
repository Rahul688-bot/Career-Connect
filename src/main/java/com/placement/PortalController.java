package com.placement;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Controller
public class PortalController {

    private final JdbcTemplate jdbc;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public PortalController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("jobCount", count("SELECT COUNT(*) FROM jobs WHERE status='OPEN'"));
        model.addAttribute("studentCount", count("SELECT COUNT(*) FROM students"));
        model.addAttribute("companyCount", count("SELECT COUNT(*) FROM companies"));
        model.addAttribute("placementCount", count("SELECT COUNT(*) FROM applications WHERE status='SELECTED'"));
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("error", null);
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("error", null);
        model.addAttribute("success", null);
        return "register";
    }

    @PostMapping("/register")
    @Transactional
    public String register(@RequestParam String role,
                           @RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam(required = false, defaultValue = "") String phone,
                           @RequestParam(required = false, defaultValue = "") String course,
                           @RequestParam(required = false, defaultValue = "0") String cgpa,
                           @RequestParam(required = false, defaultValue = "") String skills,
                           @RequestParam(required = false, defaultValue = "") String companyName,
                           @RequestParam(required = false, defaultValue = "") String industry,
                           @RequestParam(required = false, defaultValue = "") String website,
                           Model model) {

        role = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        name = name == null ? "" : name.trim();
        companyName = companyName == null ? "" : companyName.trim();

        if (!"STUDENT".equals(role) && !"COMPANY".equals(role)) {
            model.addAttribute("error", "Please select Student or Company registration.");
            return "register";
        }

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            model.addAttribute("error", "Please fill in all required fields.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        if (password.length() < 6) {
            model.addAttribute("error", "Password must contain at least 6 characters.");
            return "register";
        }

        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email=?", Integer.class, email
        );

        if (existing != null && existing > 0) {
            model.addAttribute("error", "An account with this email already exists.");
            return "register";
        }

        try {
            jdbc.update(
                    "INSERT INTO users(name, email, password, role) VALUES (?, ?, ?, ?)",
                    name, email, password, role
            );

            Integer userId = jdbc.queryForObject(
                    "SELECT id FROM users WHERE email=?", Integer.class, email
            );

            if ("STUDENT".equals(role)) {
                double studentCgpa;
                try {
                    studentCgpa = Double.parseDouble(cgpa);
                } catch (NumberFormatException e) {
                    studentCgpa = 0.0;
                }

                if (studentCgpa < 0 || studentCgpa > 10) {
                    throw new IllegalArgumentException("CGPA must be between 0 and 10.");
                }

                jdbc.update(
                        "INSERT INTO students(user_id, phone, course, cgpa, skills, resume_url) VALUES (?, ?, ?, ?, ?, '')",
                        userId, phone.trim(), course.trim(), studentCgpa, skills.trim()
                );
            } else {
                if (companyName.isBlank()) {
                    throw new IllegalArgumentException("Company name is required.");
                }

                jdbc.update(
                        "INSERT INTO companies(user_id, company_name, industry, website) VALUES (?, ?, ?, ?)",
                        userId, companyName, industry.trim(), website.trim()
                );
            }

            model.addAttribute("success", "Registration successful. You can now log in.");
            return "register";

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("error", "Registration could not be completed. Please check your details and try again.");
            return "register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        try {
            Map<String, Object> user = jdbc.queryForMap(
                    "SELECT id, name, email, password, role FROM users WHERE email=?",
                    email.trim()
            );

            String storedPassword = String.valueOf(user.get("password"));

            if (storedPassword.equals(password)) {
                session.setAttribute("uid", ((Number) user.get("id")).intValue());
                session.setAttribute("name", user.get("name"));
                session.setAttribute("role", user.get("role"));
                return "redirect:/dashboard";
            }

            model.addAttribute("error", "Invalid email or password.");
            return "login";

        } catch (EmptyResultDataAccessException e) {
            model.addAttribute("error", "Invalid email or password.");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private boolean logged(HttpSession session, String requiredRole) {
        Object uid = session.getAttribute("uid");
        Object role = session.getAttribute("role");

        if (uid == null || role == null) {
            return false;
        }

        return requiredRole == null || requiredRole.equals(String.valueOf(role));
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!logged(session, null)) {
            return "redirect:/login";
        }

        String role = String.valueOf(session.getAttribute("role"));
        String name = String.valueOf(session.getAttribute("name"));
        model.addAttribute("name", name);

        if ("ADMIN".equals(role)) {
            model.addAttribute("students",
                    count("SELECT COUNT(*) FROM students"));
            model.addAttribute("companies",
                    count("SELECT COUNT(*) FROM companies"));
            model.addAttribute("jobs",
                    count("SELECT COUNT(*) FROM jobs"));
            model.addAttribute("applications",
                    count("SELECT COUNT(*) FROM applications"));

            model.addAttribute("openJobs",
                    count("SELECT COUNT(*) FROM jobs WHERE status='OPEN'"));
            model.addAttribute("selected",
                    count("SELECT COUNT(*) FROM applications WHERE status='SELECTED'"));

            model.addAttribute("statusCounts", jdbc.queryForList(
                    "SELECT status, COUNT(*) AS total FROM applications GROUP BY status"
            ));

            model.addAttribute("recentApplications", jdbc.queryForList(
                    """
                    SELECT a.id, a.status, a.applied_at,
                           u.name AS student_name,
                           j.title AS job_title,
                           c.company_name
                    FROM applications a
                    JOIN students s ON a.student_id=s.id
                    JOIN users u ON s.user_id=u.id
                    JOIN jobs j ON a.job_id=j.id
                    JOIN companies c ON j.company_id=c.id
                    ORDER BY a.applied_at DESC
                    LIMIT 10
                    """
            ));

            model.addAttribute("allJobs", jdbc.queryForList(
                    """
                    SELECT j.id, j.title, j.location, j.package_lpa,
                           j.min_cgpa, j.status, j.deadline,
                           c.company_name
                    FROM jobs j
                    JOIN companies c ON j.company_id=c.id
                    ORDER BY j.created_at DESC
                    """
            ));

            return "admin-dashboard";
        }

        if ("STUDENT".equals(role)) {
            int uid = ((Number) session.getAttribute("uid")).intValue();

            Map<String, Object> student = jdbc.queryForMap(
                    """
                    SELECT s.*, u.name, u.email
                    FROM students s
                    JOIN users u ON s.user_id=u.id
                    WHERE s.user_id=?
                    """, uid
            );

            model.addAttribute("student", student);

            model.addAttribute("jobs", jdbc.queryForList(
                    """
                    SELECT j.id, j.title, j.description, j.location,
                           j.package_lpa, j.min_cgpa, j.skills_required,
                           j.deadline, j.status, c.company_name
                    FROM jobs j
                    JOIN companies c ON j.company_id=c.id
                    WHERE j.status='OPEN'
                    ORDER BY j.created_at DESC
                    """
            ));

            int studentId = ((Number) student.get("id")).intValue();

            model.addAttribute("applications", jdbc.queryForList(
                    """
                    SELECT a.id, a.status, a.applied_at,
                           j.title AS job_title,
                           j.package_lpa, j.location,
                           c.company_name
                    FROM applications a
                    JOIN jobs j ON a.job_id=j.id
                    JOIN companies c ON j.company_id=c.id
                    WHERE a.student_id=?
                    ORDER BY a.applied_at DESC
                    """, studentId
            ));

            return "student-dashboard";
        }

        if ("COMPANY".equals(role)) {
            int uid = ((Number) session.getAttribute("uid")).intValue();

            Map<String, Object> company = jdbc.queryForMap(
                    """
                    SELECT c.*, u.name, u.email
                    FROM companies c
                    JOIN users u ON c.user_id=u.id
                    WHERE c.user_id=?
                    """, uid
            );

            int companyId = ((Number) company.get("id")).intValue();

            model.addAttribute("company", company);

            model.addAttribute("jobs", jdbc.queryForList(
                    """
                    SELECT j.*,
                           (SELECT COUNT(*) FROM applications a
                            WHERE a.job_id=j.id) AS applicant_count
                    FROM jobs j
                    WHERE j.company_id=?
                    ORDER BY j.created_at DESC
                    """, companyId
            ));

            model.addAttribute("applicants", jdbc.queryForList(
                    """
                    SELECT a.id, a.status, a.applied_at,
                           u.name AS student_name, u.email,
                           s.phone, s.course, s.cgpa, s.skills,
                           j.title AS job_title
                    FROM applications a
                    JOIN students s ON a.student_id=s.id
                    JOIN users u ON s.user_id=u.id
                    JOIN jobs j ON a.job_id=j.id
                    WHERE j.company_id=?
                    ORDER BY a.applied_at DESC
                    """, companyId
            ));

            return "company-dashboard";
        }

        return "redirect:/logout";
    }

    @PostMapping("/apply/{jobId}")
    public String apply(@PathVariable int jobId, HttpSession session) {
        if (!logged(session, "STUDENT")) {
            return "redirect:/login";
        }

        int uid = ((Number) session.getAttribute("uid")).intValue();

        try {
            Map<String, Object> student = jdbc.queryForMap(
                    "SELECT id, cgpa FROM students WHERE user_id=?", uid
            );

            Map<String, Object> job = jdbc.queryForMap(
                    "SELECT min_cgpa FROM jobs WHERE id=? AND status='OPEN'", jobId
            );

            double cgpa = ((Number) student.get("cgpa")).doubleValue();
            double minCgpa = ((Number) job.get("min_cgpa")).doubleValue();

            if (cgpa >= minCgpa) {
                int studentId = ((Number) student.get("id")).intValue();

                jdbc.update(
                        """
                        INSERT IGNORE INTO applications(student_id, job_id, status)
                        VALUES (?, ?, 'APPLIED')
                        """,
                        studentId, jobId
                );
            }

        } catch (EmptyResultDataAccessException ignored) {
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/application/{id}/status")
    public String updateApplicationStatus(@PathVariable int id,
                                           @RequestParam String status,
                                           HttpSession session) {

        String role = String.valueOf(session.getAttribute("role"));

        if (!"ADMIN".equals(role) && !"COMPANY".equals(role)) {
            return "redirect:/login";
        }

        List<String> valid = List.of(
                "APPLIED", "SHORTLISTED", "INTERVIEW", "SELECTED", "REJECTED"
        );

        if (valid.contains(status)) {
            if ("ADMIN".equals(role)) {
                jdbc.update("UPDATE applications SET status=? WHERE id=?", status, id);
            } else {
                int uid = ((Number) session.getAttribute("uid")).intValue();

                jdbc.update(
                        """
                        UPDATE applications a
                        JOIN jobs j ON a.job_id=j.id
                        JOIN companies c ON j.company_id=c.id
                        SET a.status=?
                        WHERE a.id=? AND c.user_id=?
                        """,
                        status, id, uid
                );
            }
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/jobs/add")
    public String addJob(@RequestParam String title,
                          @RequestParam String description,
                          @RequestParam String location,
                          @RequestParam double package_lpa,
                          @RequestParam double min_cgpa,
                          @RequestParam String skills_required,
                          @RequestParam String deadline,
                          HttpSession session) {

        if (!logged(session, "COMPANY")) {
            return "redirect:/login";
        }

        int uid = ((Number) session.getAttribute("uid")).intValue();

        try {
            int companyId = jdbc.queryForObject(
                    "SELECT id FROM companies WHERE user_id=?",
                    Integer.class, uid
            );

            jdbc.update(
                    """
                    INSERT INTO jobs
                    (company_id, title, description, location, package_lpa,
                     min_cgpa, skills_required, deadline, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')
                    """,
                    companyId, title, description, location,
                    package_lpa, min_cgpa, skills_required, deadline
            );
        } catch (Exception ignored) {
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/jobs/{id}/close")
    public String closeJob(@PathVariable int id, HttpSession session) {
        if (!logged(session, "COMPANY")) {
            return "redirect:/login";
        }

        int uid = ((Number) session.getAttribute("uid")).intValue();

        jdbc.update(
                """
                UPDATE jobs j
                JOIN companies c ON j.company_id=c.id
                SET j.status='CLOSED'
                WHERE j.id=? AND c.user_id=?
                """,
                id, uid
        );

        return "redirect:/dashboard";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String phone,
                                @RequestParam String course,
                                @RequestParam double cgpa,
                                @RequestParam String skills,
                                HttpSession session) {

        if (!logged(session, "STUDENT")) {
            return "redirect:/login";
        }

        int uid = ((Number) session.getAttribute("uid")).intValue();

        jdbc.update(
                """
                UPDATE students
                SET phone=?, course=?, cgpa=?, skills=?
                WHERE user_id=?
                """,
                phone, course, cgpa, skills, uid
        );

        return "redirect:/dashboard";
    }

    @PostMapping("/resume/upload")
    public String uploadResume(@RequestParam("resume") MultipartFile file,
                               HttpSession session) {

        if (!logged(session, "STUDENT")) {
            return "redirect:/login";
        }

        if (file == null || file.isEmpty()) {
            return "redirect:/dashboard";
        }

        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "resume");
        String lower = original.toLowerCase(Locale.ROOT);

        if (!(lower.endsWith(".pdf")
                || lower.endsWith(".doc")
                || lower.endsWith(".docx"))) {
            return "redirect:/dashboard";
        }

        try {
            Path directory = Paths.get(uploadDir);
            Files.createDirectories(directory);

            String safeName = UUID.randomUUID() + "_" +
                    Paths.get(original).getFileName();

            Path target = directory.resolve(safeName);

            Files.copy(file.getInputStream(), target,
                    StandardCopyOption.REPLACE_EXISTING);

            int uid = ((Number) session.getAttribute("uid")).intValue();

            jdbc.update(
                    "UPDATE students SET resume_url=? WHERE user_id=?",
                    safeName, uid
            );

        } catch (IOException ignored) {
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/resume")
    @ResponseBody
    public byte[] downloadResume(HttpSession session) throws IOException {

        if (!logged(session, "STUDENT")) {
            return new byte[0];
        }

        int uid = ((Number) session.getAttribute("uid")).intValue();

        String filename = jdbc.queryForObject(
                "SELECT resume_url FROM students WHERE user_id=?",
                String.class, uid
        );

        if (filename == null || filename.isBlank()) {
            return new byte[0];
        }

        Path file = Paths.get(uploadDir).resolve(filename);

        if (!Files.exists(file)) {
            return new byte[0];
        }

        return Files.readAllBytes(file);
    }

    @PostMapping("/admin/job/{id}/delete")
    public String deleteJob(@PathVariable int id, HttpSession session) {
        if (!logged(session, "ADMIN")) {
            return "redirect:/login";
        }

        jdbc.update("DELETE FROM jobs WHERE id=?", id);
        return "redirect:/dashboard";
    }

    @PostMapping("/admin/job/{id}/toggle")
    public String toggleJob(@PathVariable int id, HttpSession session) {
        if (!logged(session, "ADMIN")) {
            return "redirect:/login";
        }

        jdbc.update(
                """
                UPDATE jobs
                SET status = CASE
                    WHEN status='OPEN' THEN 'CLOSED'
                    ELSE 'OPEN'
                END
                WHERE id=?
                """,
                id
        );

        return "redirect:/dashboard";
    }

    private int count(String sql) {
        Integer result = jdbc.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }
}
