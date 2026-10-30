USE placement_db;

INSERT IGNORE INTO users
    (id, name, email, password, role)
VALUES
    (1, 'System Administrator', 'admin@careerconnect.com', 'admin123', 'ADMIN'),
    (2, 'Rahul Sharma', 'rahul@student.com', 'student123', 'STUDENT'),
    (3, 'Aarav Mehta', 'aarav@student.com', 'student123', 'STUDENT'),
    (4, 'TechNova Solutions', 'hr@technova.com', 'company123', 'COMPANY'),
    (5, 'InnoByte Technologies', 'hr@innobyte.com', 'company123', 'COMPANY');

INSERT IGNORE INTO students
    (id, user_id, phone, course, cgpa, skills, resume_url)
VALUES
    (1, 2, '9876543210', 'Bachelor of Computer Science', 8.70,
     'Java, Spring Boot, MySQL, HTML, CSS, JavaScript', ''),
    (2, 3, '9876501234', 'Bachelor of Computer Science', 8.20,
     'Python, SQL, JavaScript, React, Git', '');

INSERT IGNORE INTO companies
    (id, user_id, company_name, industry, website)
VALUES
    (1, 4, 'TechNova Solutions', 'Software & IT', 'https://example.com'),
    (2, 5, 'InnoByte Technologies', 'Technology', 'https://example.com');

INSERT IGNORE INTO jobs
    (id, company_id, title, description, location, package_lpa,
     min_cgpa, skills_required, deadline, status)
VALUES
    (1, 1, 'Junior Java Developer',
     'Build scalable web applications using Java and Spring Boot.',
     'Pune', 6.50, 7.00, 'Java, Spring Boot, MySQL',
     DATE_ADD(CURDATE(), INTERVAL 20 DAY), 'OPEN'),

    (2, 1, 'Software Engineer Intern',
     'Work with a product engineering team on real-world applications.',
     'Bengaluru', 4.50, 6.50, 'JavaScript, HTML, CSS, SQL',
     DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'OPEN'),

    (3, 2, 'Backend Developer',
     'Develop REST APIs and database-driven services.',
     'Mumbai', 7.20, 7.50, 'Java, SQL, REST API',
     DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'OPEN'),

    (4, 2, 'Graduate Software Trainee',
     'Entry-level role with structured technical training.',
     'Hyderabad', 5.00, 6.00, 'Programming, DBMS, OOP',
     DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'OPEN');
