-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: localhost    Database: nova_1
-- ------------------------------------------------------
-- Server version	8.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `answer_option`
--

DROP TABLE IF EXISTS `answer_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `answer_option` (
  `answer_option_id` int NOT NULL AUTO_INCREMENT,
  `question_id` int NOT NULL,
  `title` text,
  `correct_answer` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`answer_option_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `answer_option_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `answer_option`
--

LOCK TABLES `answer_option` WRITE;
/*!40000 ALTER TABLE `answer_option` DISABLE KEYS */;
/*!40000 ALTER TABLE `answer_option` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class`
--

DROP TABLE IF EXISTS `class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class` (
  `class_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `teacher_id` int DEFAULT NULL,
  `class_name` varchar(255) DEFAULT NULL,
  `start_date` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `end_date` datetime(6) DEFAULT NULL,
  `schedule_type` varchar(50) DEFAULT NULL,
  `schedule` varchar(255) DEFAULT NULL,
  `slot_time` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`class_id`),
  KEY `course_id` (`course_id`),
  KEY `teacher_id` (`teacher_id`),
  CONSTRAINT `class_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `class_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class`
--

LOCK TABLES `class` WRITE;
/*!40000 ALTER TABLE `class` DISABLE KEYS */;
INSERT INTO `class` VALUES (101,101,103,'IELTS-K01','2026-04-01 18:00:00.000000','Open',NULL,NULL,'T2 - T4 - T6 (18:00 - 20:00)',NULL),(102,101,103,'IELTS-K02','2026-04-15 19:30:00.000000','Open',NULL,NULL,'T3 - T5 - T7 (19:30 - 21:30)',NULL),(103,102,103,'TOEIC-K01','2026-03-20 18:00:00.000000','Closed',NULL,NULL,NULL,NULL),(201,205,204,'èiọdioks','2024-03-15 00:00:00.000000','Open','2024-06-15 00:00:00.000000','Thứ 2-4-6','Thứ 3-5-7','18:00 - 20:00'),(202,201,205,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(203,202,204,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(204,203,205,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(205,201,204,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(206,201,205,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(207,201,205,'I7','2026-03-02 04:13:00.000000',NULL,'2026-03-20 04:13:00.000000',NULL,'Thứ 2-4-6','08:00 - 10:00'),(208,201,1,NULL,'2026-04-02 04:20:00.000000',NULL,'2026-04-09 04:27:00.000000',NULL,'Thứ 2-4-6','08:00 - 10:00'),(209,203,1,'aaâ','2026-03-25 14:45:00.000000','Open',NULL,NULL,'Thứ 2-4-6','08:00 - 10:00'),(210,203,213,'IL157','2026-03-18 14:57:00.000000','Open',NULL,NULL,'Thứ 2-4-6','18:00 - 20:00'),(211,204,204,'jwio','2026-03-26 08:59:00.000000','Pending',NULL,NULL,'Thứ 2-4-6','08:00 - 10:00');
/*!40000 ALTER TABLE `class` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `course_id` int NOT NULL AUTO_INCREMENT,
  `course_name` varchar(255) DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `expert_id` int DEFAULT NULL,
  `manager_id` int DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `description` text,
  `image_url` varchar(255) DEFAULT NULL,
  `price` double DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`course_id`),
  KEY `category_id` (`category_id`),
  KEY `expert_id` (`expert_id`),
  KEY `manager_id` (`manager_id`),
  CONSTRAINT `course_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `setting` (`setting_id`),
  CONSTRAINT `course_ibfk_2` FOREIGN KEY (`expert_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `course_ibfk_3` FOREIGN KEY (`manager_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course`
--

LOCK TABLES `course` WRITE;
/*!40000 ALTER TABLE `course` DISABLE KEYS */;
INSERT INTO `course` VALUES (101,NULL,101,102,101,'Active','Khóa học nền tảng IELTS 5.0','/assets/img/education/courses-13.webp',6000000,'IELTS Foundation'),(102,NULL,102,102,101,'Active','Chinh phục TOEIC 500+ trong 2 tháng','/assets/img/education/students-9.webp',4000000,'TOEIC 500+'),(201,'Luyện thi IELTS Academic',206,203,202,'ACTIVE',NULL,NULL,NULL,NULL),(202,'Luyện thi TOEIC 2 Kỹ năng',207,203,202,'ACTIVE',NULL,NULL,NULL,NULL),(203,'Tiếng Anh Giao tiếp Phản xạ',208,203,202,'ACTIVE',NULL,NULL,NULL,NULL),(204,'topic123456',NULL,NULL,NULL,'active','',NULL,500000,'sl'),(205,'Msklưenakl',NULL,NULL,NULL,'active','bnvshjacvkjhsagvdhgdjhghhksjfdhjl',NULL,5000000,'IL258');
/*!40000 ALTER TABLE `course` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `email_verifications`
--

DROP TABLE IF EXISTS `email_verifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_verifications` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `expiry_time` datetime(6) NOT NULL,
  `verification_code` varchar(250) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `email_verifications`
--

LOCK TABLES `email_verifications` WRITE;
/*!40000 ALTER TABLE `email_verifications` DISABLE KEYS */;
INSERT INTO `email_verifications` VALUES (2,'maichithanh317@gmail.com','2026-02-10 07:37:37.177372','e9e4b4ab-2021-4422-881f-54b774470536'),(3,'xuantaizxc22@gmail.com','2026-03-08 08:29:25.538617','132331');
/*!40000 ALTER TABLE `email_verifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lesson`
--

DROP TABLE IF EXISTS `lesson`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lesson` (
  `lesson_id` int NOT NULL AUTO_INCREMENT,
  `module_id` int NOT NULL,
  `order_index` int DEFAULT NULL,
  `content_text` varchar(255) DEFAULT NULL,
  `duration` varchar(255) DEFAULT NULL,
  `lesson_name` varchar(255) DEFAULT NULL,
  `quiz_id` int DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `video_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`lesson_id`),
  KEY `module_id` (`module_id`),
  KEY `fk_lesson_quiz` (`quiz_id`),
  CONSTRAINT `fk_lesson_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`),
  CONSTRAINT `lesson_ibfk_1` FOREIGN KEY (`module_id`) REFERENCES `module` (`module_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lesson`
--

LOCK TABLES `lesson` WRITE;
/*!40000 ALTER TABLE `lesson` DISABLE KEYS */;
INSERT INTO `lesson` VALUES (1,1,1,NULL,'15:30','Bài 1: Tổng quan về IELTS Listening',NULL,'VIDEO','https://youtube.com/embed/xyz123'),(2,1,2,NULL,'20:00','Bài 2: Kỹ năng bắt Keywords',NULL,'VIDEO','https://youtube.com/embed/abc456'),(3,1,3,'Nội dung chi tiết tài liệu học tập môn Nghe...','5 Trang','Tài liệu: Các bẫy thường gặp trong Section 1',NULL,'DOC',NULL),(4,1,4,NULL,'10 Câu hỏi','Quiz: Test Listening Section 1',NULL,'QUIZ',NULL),(5,2,1,NULL,'18:45','Bài 1: Kỹ năng Skimming & Scanning',NULL,'VIDEO','https://youtube.com/embed/def789'),(6,2,2,'Đoạn văn bài tập Reading...','Bài tập thực hành','Bài tập: Luyện đọc hiểu True/False/Not Given',NULL,'DOC',NULL),(7,4,1,NULL,'12:00','Cách làm dạng bài tranh tả người',NULL,'VIDEO','https://youtube.com/embed/toeic123'),(201,201,1,NULL,NULL,NULL,NULL,NULL,NULL),(202,201,2,NULL,NULL,NULL,NULL,NULL,NULL),(203,202,1,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `lesson` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `module`
--

DROP TABLE IF EXISTS `module`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `module` (
  `module_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `order_index` int DEFAULT NULL,
  `module_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`module_id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `module_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=203 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `module`
--

LOCK TABLES `module` WRITE;
/*!40000 ALTER TABLE `module` DISABLE KEYS */;
INSERT INTO `module` VALUES (1,101,1,'Chương 1: Listening Foundation (Nền tảng Nghe)'),(2,101,2,'Chương 2: Reading Foundation (Nền tảng Đọc)'),(3,101,3,'Chương 3: Speaking & Writing Basics'),(4,102,1,'Chương 1: Part 1 - Photographs'),(201,201,1,NULL),(202,201,2,NULL);
/*!40000 ALTER TABLE `module` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_tokens`
--

DROP TABLE IF EXISTS `password_reset_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
  `id` int NOT NULL AUTO_INCREMENT,
  `expiry_datetime` datetime(6) NOT NULL,
  `is_used` bit(1) DEFAULT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK20xweju6fxkxcx3taa9elhtew` (`user_id`),
  CONSTRAINT `FK20xweju6fxkxcx3taa9elhtew` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_tokens`
--

LOCK TABLES `password_reset_tokens` WRITE;
/*!40000 ALTER TABLE `password_reset_tokens` DISABLE KEYS */;
INSERT INTO `password_reset_tokens` VALUES (6,'2026-03-03 16:44:22.019044',_binary '\0','af3091c7-6a61-4f33-9064-85694c264609',2);
/*!40000 ALTER TABLE `password_reset_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question`
--

DROP TABLE IF EXISTS `question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question` (
  `question_id` int NOT NULL AUTO_INCREMENT,
  `module_id` int NOT NULL,
  `user_id` int DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`question_id`),
  KEY `module_id` (`module_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `question_ibfk_1` FOREIGN KEY (`module_id`) REFERENCES `module` (`module_id`),
  CONSTRAINT `question_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question`
--

LOCK TABLES `question` WRITE;
/*!40000 ALTER TABLE `question` DISABLE KEYS */;
/*!40000 ALTER TABLE `question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question_group`
--

DROP TABLE IF EXISTS `question_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_group` (
  `group_id` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question_group`
--

LOCK TABLES `question_group` WRITE;
/*!40000 ALTER TABLE `question_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `question_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quiz`
--

DROP TABLE IF EXISTS `quiz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz` (
  `quiz_id` int NOT NULL AUTO_INCREMENT,
  `course_id` int NOT NULL,
  `user_id` int DEFAULT NULL,
  `class_id` int DEFAULT NULL,
  `quiz_category` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`quiz_id`),
  KEY `course_id` (`course_id`),
  KEY `user_id` (`user_id`),
  KEY `class_id` (`class_id`),
  CONSTRAINT `quiz_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `quiz_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `quiz_ibfk_3` FOREIGN KEY (`class_id`) REFERENCES `class` (`class_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz`
--

LOCK TABLES `quiz` WRITE;
/*!40000 ALTER TABLE `quiz` DISABLE KEYS */;
INSERT INTO `quiz` VALUES (201,201,203,201,'ENTRY_TEST');
/*!40000 ALTER TABLE `quiz` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quiz_answer`
--

DROP TABLE IF EXISTS `quiz_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_answer` (
  `answer_id` int NOT NULL AUTO_INCREMENT,
  `result_id` int NOT NULL,
  `question_id` int NOT NULL,
  `answered_options` text,
  PRIMARY KEY (`answer_id`),
  KEY `result_id` (`result_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `quiz_answer_ibfk_1` FOREIGN KEY (`result_id`) REFERENCES `quiz_result` (`result_id`),
  CONSTRAINT `quiz_answer_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz_answer`
--

LOCK TABLES `quiz_answer` WRITE;
/*!40000 ALTER TABLE `quiz_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `quiz_answer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quiz_config`
--

DROP TABLE IF EXISTS `quiz_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_config` (
  `quiz_id` int NOT NULL,
  `module_id` int NOT NULL,
  `num_of_question` int DEFAULT NULL,
  PRIMARY KEY (`quiz_id`,`module_id`),
  KEY `module_id` (`module_id`),
  CONSTRAINT `quiz_config_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`),
  CONSTRAINT `quiz_config_ibfk_2` FOREIGN KEY (`module_id`) REFERENCES `module` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz_config`
--

LOCK TABLES `quiz_config` WRITE;
/*!40000 ALTER TABLE `quiz_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `quiz_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quiz_question`
--

DROP TABLE IF EXISTS `quiz_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_question` (
  `quiz_question_id` int NOT NULL AUTO_INCREMENT,
  `quiz_id` int NOT NULL,
  `question_id` int NOT NULL,
  `group_id` int DEFAULT NULL,
  PRIMARY KEY (`quiz_question_id`),
  KEY `quiz_id` (`quiz_id`),
  KEY `question_id` (`question_id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `quiz_question_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`),
  CONSTRAINT `quiz_question_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`),
  CONSTRAINT `quiz_question_ibfk_3` FOREIGN KEY (`group_id`) REFERENCES `question_group` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz_question`
--

LOCK TABLES `quiz_question` WRITE;
/*!40000 ALTER TABLE `quiz_question` DISABLE KEYS */;
/*!40000 ALTER TABLE `quiz_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quiz_result`
--

DROP TABLE IF EXISTS `quiz_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_result` (
  `result_id` int NOT NULL AUTO_INCREMENT,
  `quiz_id` int NOT NULL,
  `user_id` int NOT NULL,
  `correct_rate` decimal(5,2) DEFAULT NULL,
  PRIMARY KEY (`result_id`),
  KEY `quiz_id` (`quiz_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `quiz_result_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`),
  CONSTRAINT `quiz_result_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=203 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz_result`
--

LOCK TABLES `quiz_result` WRITE;
/*!40000 ALTER TABLE `quiz_result` DISABLE KEYS */;
INSERT INTO `quiz_result` VALUES (201,201,206,85.00),(202,201,207,45.50);
/*!40000 ALTER TABLE `quiz_result` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registration`
--

DROP TABLE IF EXISTS `registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registration` (
  `registration_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `class_id` int NOT NULL,
  `course_id` int NOT NULL,
  `registration_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `registration_price` decimal(38,2) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `note` text,
  PRIMARY KEY (`registration_id`),
  KEY `user_id` (`user_id`),
  KEY `class_id` (`class_id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `registration_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `registration_ibfk_2` FOREIGN KEY (`class_id`) REFERENCES `class` (`class_id`),
  CONSTRAINT `registration_ibfk_3` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registration`
--

LOCK TABLES `registration` WRITE;
/*!40000 ALTER TABLE `registration` DISABLE KEYS */;
INSERT INTO `registration` VALUES (101,2,101,101,'2026-02-20 10:00:00',5000000.00,'Cancelled','Đăng ký chờ duyệt (Test Hủy)'),(102,2,103,102,'2026-01-15 08:30:00',3500000.00,'Approved','Đã thanh toán (Test My Courses)'),(103,2,102,101,'2025-12-01 14:00:00',5000000.00,'Cancelled','Hủy do bận lịch (Test hiển thị)'),(201,206,201,201,'2026-03-02 10:49:44',5500000.00,'PAID',NULL),(202,207,201,201,'2026-03-02 10:49:44',5500000.00,'PAID',NULL),(203,208,202,201,'2026-03-02 10:49:44',5200000.00,'PAID',NULL),(204,206,203,202,'2026-03-02 10:49:44',3000000.00,'PAID',NULL);
/*!40000 ALTER TABLE `registration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `setting`
--

DROP TABLE IF EXISTS `setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setting` (
  `setting_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `setting_type` varchar(255) DEFAULT NULL,
  `order_index` int DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`setting_id`)
) ENGINE=InnoDB AUTO_INCREMENT=213 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `setting`
--

LOCK TABLES `setting` WRITE;
/*!40000 ALTER TABLE `setting` DISABLE KEYS */;
INSERT INTO `setting` VALUES (101,'IELTS Preparation','CAT_IELTS','COURSE_CATEGORY',NULL,'Active',NULL),(102,'TOEIC Preparation','CAT_TOEIC','COURSE_CATEGORY',NULL,'Active',NULL),(201,'ROLE_ADMIN',NULL,'ROLE',NULL,'ACTIVE','Quản trị viên'),(202,'ROLE_MANAGER',NULL,'ROLE',NULL,'ACTIVE','Quản lý đào tạo'),(203,'ROLE_EXPERT',NULL,'ROLE',NULL,'ACTIVE','Chuyên gia nội dung'),(204,'ROLE_TEACHER',NULL,'ROLE',NULL,'ACTIVE','Giảng viên'),(205,'ROLE_STUDENT',NULL,'ROLE',NULL,'ACTIVE','Học viên'),(206,'IELTS Academic',NULL,'COURSE_CATEGORY',NULL,'ACTIVE','Luyện thi IELTS'),(207,'TOEIC 2 Kỹ năng',NULL,'COURSE_CATEGORY',NULL,'ACTIVE','Luyện thi TOEIC'),(208,'English Communication',NULL,'COURSE_CATEGORY',NULL,'ACTIVE','Giao tiếp phản xạ'),(209,'Admin','ROLE_ADMIN','USER_ROLE',1,'Active','System Administrator'),(210,'Student','ROLE_STUDENT','USER_ROLE',2,'Active','Learner'),(211,'Teacher','ROLE_TEACHER','USER_ROLE',3,'Active','Instructor'),(212,'Manager','ROLE_MANAGER','USER_ROLE',4,'Active','Course & Staff Manager');
/*!40000 ALTER TABLE `setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role_id` int DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `last_login` datetime DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `note` text,
  `auth_provider` varchar(255) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `provider_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `setting` (`setting_id`)
) ENGINE=InnoDB AUTO_INCREMENT=219 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'System Administrator','admin@novalms.edu.vn','0769169665','$2a$10$vdmjbVXyCn3TijMcNjQ4/.ufegsHt3LTqWvRSCgXFILoec6xQXKgK',201,'https://res.cloudinary.com/decqv3ed0/image/upload/v1772262961/b8pukgr9jkkmkthqzgwd.png',NULL,'Active','fgfffdfdf','LOCAL',NULL,NULL,NULL),(2,'Nart C','thanhmche172833@fpt.edu.vn','098761234','$2a$10$KxSQvUZjeRye0nP3tG/Q1ueLpwj3o2uYGyQoeYx4Db7roZqBXU256',205,'https://lh3.googleusercontent.com/a/ACg8ocKSEv_g3GPLFxJGd8kEfncwPnHNjletUQZcbNlhmCntdBwW4x7k=s96-c','2026-03-04 14:50:14','Active','sdsd123 2','GOOGLE',NULL,NULL,NULL),(3,'NGUYEN LONG THANH','maichithanh317@gmail.com','0989878056','$2a$10$QDtwVvyiVmVq3TXgES7pgOQcVwYkXSxGeem1S7UwMt05tphP36l0O',205,NULL,NULL,'Active',NULL,'LOCAL','Hanoi','Male',NULL),(4,'C nẹt','maingaquynhanh@gmail.com',NULL,'$2a$10$NrqkZCYOY6WFuVzzAH4Ww.rks7.VH0qrrKu4v9xPFwl1JzMlwV6wG',205,'https://lh3.googleusercontent.com/a/ACg8ocLDzs7bIK3rqgxTOnHWfq40sL3LiRIHs6_AdH0E4vrFfzHL-1n6=s96-c','2026-02-10 08:01:19','Active',NULL,'GOOGLE',NULL,NULL,NULL),(101,'Manager Admin','manager@novalms.com',NULL,'$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1ipc9Z5i2q.zcyFm',NULL,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(102,'Expert AI','expert@novalms.com',NULL,'$2a$10$n/V.Xn6.Bf182N58MOnz3eE8J4.aTzHlD85t/W7R37yY8Zt9eK6/m',NULL,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(103,'Teacher John','teacher@novalms.com',NULL,'$2a$10$n/V.Xn6.Bf182N58MOnz3eE8J4.aTzHlD85t/W7R37yY8Zt9eK6/m',NULL,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(104,'Course Manager','manager@novalms.edu.vn',NULL,'$2a$10$ktuVxFZOhBw7PTz8kwmNYOhszbs7c0hjRihB98uKFg.jYx2USyEba',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(201,'System Admin','admin@center.com',NULL,'$2a$10$R9h/lSAbvI7.7H7.pW4nbeNfQnQNbM3.4n.G1n2n3n4n5n6n7n8n9',201,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(202,'Manager Nguyen','manager@center.com',NULL,'$2a$10$R9h/lSAbvI7.7H7.pW4nbeNfQnQNbM3.4n.G1n2n3n4n5n6n7n8n9',202,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(203,'Expert Tran','expert@center.com',NULL,'$2a$10$R9h/lSAbvI7.7H7.pW4nbeNfQnQNbM3.4n.G1n2n3n4n5n6n7n8n9',203,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(204,'Teacher Hoang','teacher1@center.com',NULL,'$2a$10$R9h/lSAbvI7.7H7.pW4nbeNfQnQNbM3.4n.G1n2n3n4n5n6n7n8n9',204,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(205,'Teacher Le','teacher2@center.com',NULL,'$2a$10$R9h/lSAbvI7.7H7.pW4nbeNfQnQNbM3.4n.G1n2n3n4n5n6n7n8n9',204,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(206,'Student An','student1@gmail.com',NULL,'$2a$10$/GCZ43crUE8m3nyx/2Bcn.iRDl0NkIKL.r/xE4SX8tYTHQkotJWcS',205,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(207,'Student Binh','student2@gmail.com',NULL,'$2a$10$R9h/lSAbvI7.7H7.pW4nbeNfQnQNbM3.4n.G1n2n3n4n5n6n7n8n9',205,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(208,'Student Cuong','student3@gmail.com',NULL,'$2a$10$R9h/lSAbvI7.7H7.pW4nbeNfQnQNbM3.4n.G1n2n3n4n5n6n7n8n9',205,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(211,'Nguyễn Chu Tứ','chutu1212141@gmail.com','0968515656','default_password',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(212,'Nguyen Chu Tu K17 HL','tunche170493@fpt.edu.vn',NULL,'$2a$10$O933MpnUk8nwIxwzTtmoE.5PkIA9TQQEJDPlpn9X/GjAhth1Zn66C',205,'https://lh3.googleusercontent.com/a/ACg8ocI4LEjoC6mlg2U8Sx2ap3u87o-h4FViSEAkV472XdjSG8XM=s96-c','2026-03-04 13:31:51','Active',NULL,'GOOGLE',NULL,NULL,NULL),(213,'Course Manager','student@novalms.edu.vn',NULL,'$2a$10$mcqwmCrMwMH13AhWVTJZkeV5O/OlEuvTKwJ1AJEYadCz2UyKcoTOa',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(216,'Nguyễn Chu Tứ','chutu@gmail.com','0968716203','default123',201,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(217,'Nguyễn Chu Tứ','chu@gmail.com','0968771659','default123',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(218,'Nguyễn Chu Tứ','chu56@gmail.com','6659896238','default123',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_lesson`
--

DROP TABLE IF EXISTS `user_lesson`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_lesson` (
  `user_id` int NOT NULL,
  `lesson_id` int NOT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`,`lesson_id`),
  KEY `lesson_id` (`lesson_id`),
  CONSTRAINT `user_lesson_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `user_lesson_ibfk_2` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`lesson_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_lesson`
--

LOCK TABLES `user_lesson` WRITE;
/*!40000 ALTER TABLE `user_lesson` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_lesson` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-08 17:15:14
