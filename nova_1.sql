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
  `match_target` varchar(500) DEFAULT NULL,
  `order_index` int DEFAULT NULL,
  PRIMARY KEY (`answer_option_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `answer_option_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=245 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `answer_option`
--

LOCK TABLES `answer_option` WRITE;
/*!40000 ALTER TABLE `answer_option` DISABLE KEYS */;
INSERT INTO `answer_option` VALUES (1,1,'Option A',1,NULL,NULL),(2,1,'Option B',0,NULL,NULL),(3,1,'Option C',0,NULL,NULL),(4,1,'Option D',0,NULL,NULL),(5,2,'Item 1',1,'Description 1',NULL),(6,2,'Item 2',1,'Description 2',NULL),(7,3,'target_word',1,NULL,NULL),(8,4,'Option 1',1,NULL,NULL),(9,4,'Option 2',0,NULL,NULL),(10,4,'Option 3',1,NULL,NULL),(11,5,'target_word',1,NULL,NULL),(12,6,'Option A',1,NULL,NULL),(13,6,'Option B',0,NULL,NULL),(14,6,'Option C',0,NULL,NULL),(15,6,'Option D',0,NULL,NULL),(16,7,'Option 1',1,NULL,NULL),(17,7,'Option 2',0,NULL,NULL),(18,7,'Option 3',1,NULL,NULL),(19,8,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(20,9,'target_word',1,NULL,NULL),(21,10,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(22,11,'target_word',1,NULL,NULL),(23,12,'Option A',1,NULL,NULL),(24,12,'Option B',0,NULL,NULL),(25,12,'Option C',0,NULL,NULL),(26,12,'Option D',0,NULL,NULL),(27,13,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(28,14,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(29,15,'Option A',1,NULL,NULL),(30,15,'Option B',0,NULL,NULL),(31,15,'Option C',0,NULL,NULL),(32,15,'Option D',0,NULL,NULL),(33,16,'Option 1',1,NULL,NULL),(34,16,'Option 2',0,NULL,NULL),(35,16,'Option 3',1,NULL,NULL),(36,17,'Option A',1,NULL,NULL),(37,17,'Option B',0,NULL,NULL),(38,17,'Option C',0,NULL,NULL),(39,17,'Option D',0,NULL,NULL),(40,18,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(41,19,'target_word',1,NULL,NULL),(42,20,'Option 1',1,NULL,NULL),(43,20,'Option 2',0,NULL,NULL),(44,20,'Option 3',1,NULL,NULL),(45,21,'target_word',1,NULL,NULL),(46,22,'Item 1',1,'Description 1',NULL),(47,22,'Item 2',1,'Description 2',NULL),(48,23,'Item 1',1,'Description 1',NULL),(49,23,'Item 2',1,'Description 2',NULL),(50,24,'target_word',1,NULL,NULL),(51,25,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(52,26,'target_word',1,NULL,NULL),(53,27,'Option 1',1,NULL,NULL),(54,27,'Option 2',0,NULL,NULL),(55,27,'Option 3',1,NULL,NULL),(56,28,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(57,29,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(58,30,'Sample perfect answer / rubric criterion.',1,NULL,NULL),(59,31,'answer',1,NULL,NULL),(60,32,'answer',1,NULL,NULL),(61,33,'Option 1 (Correct)',1,NULL,NULL),(62,33,'Option 2 (Correct)',1,NULL,NULL),(63,33,'Option 3',0,NULL,NULL),(64,34,'A',1,'1',NULL),(65,34,'B',1,'2',NULL),(66,35,'answer',1,NULL,NULL),(67,36,'Option 1 (Correct)',1,NULL,NULL),(68,36,'Option 2 (Correct)',1,NULL,NULL),(69,36,'Option 3',0,NULL,NULL),(70,37,'Option 1 (Correct)',1,NULL,NULL),(71,37,'Option 2 (Correct)',1,NULL,NULL),(72,37,'Option 3',0,NULL,NULL),(73,38,'Correct Option',1,NULL,NULL),(74,38,'Wrong Option',0,NULL,NULL),(75,39,'Correct Option',1,NULL,NULL),(76,39,'Wrong Option',0,NULL,NULL),(77,40,'answer',1,NULL,NULL),(78,41,'answer',1,NULL,NULL),(79,42,'answer',1,NULL,NULL),(80,43,'Option 1 (Correct)',1,NULL,NULL),(81,43,'Option 2 (Correct)',1,NULL,NULL),(82,43,'Option 3',0,NULL,NULL),(83,44,'answer',1,NULL,NULL),(84,45,'answer',1,NULL,NULL),(85,46,'Option 1 (Correct)',1,NULL,NULL),(86,46,'Option 2 (Correct)',1,NULL,NULL),(87,46,'Option 3',0,NULL,NULL),(88,47,'Option 1 (Correct)',1,NULL,NULL),(89,47,'Option 2 (Correct)',1,NULL,NULL),(90,47,'Option 3',0,NULL,NULL),(91,48,'A',1,'1',NULL),(92,48,'B',1,'2',NULL),(93,49,'A',1,'1',NULL),(94,49,'B',1,'2',NULL),(95,50,'A',1,'1',NULL),(96,50,'B',1,'2',NULL),(97,51,'answer',1,NULL,NULL),(98,52,'Option 1 (Correct)',1,NULL,NULL),(99,52,'Option 2 (Correct)',1,NULL,NULL),(100,52,'Option 3',0,NULL,NULL),(101,53,'Option 1 (Correct)',1,NULL,NULL),(102,53,'Option 2 (Correct)',1,NULL,NULL),(103,53,'Option 3',0,NULL,NULL),(104,54,'answer',1,NULL,NULL),(105,55,'Option 1 (Correct)',1,NULL,NULL),(106,55,'Option 2 (Correct)',1,NULL,NULL),(107,55,'Option 3',0,NULL,NULL),(108,56,'Correct Option',1,NULL,NULL),(109,56,'Wrong Option',0,NULL,NULL),(110,57,'Correct Option',1,NULL,NULL),(111,57,'Wrong Option',0,NULL,NULL),(112,58,'A',1,'1',NULL),(113,58,'B',1,'2',NULL),(114,59,'Correct Option',1,NULL,NULL),(115,59,'Wrong Option',0,NULL,NULL),(116,60,'A',1,'1',NULL),(117,60,'B',1,'2',NULL),(118,61,'Option 1 (Correct)',1,NULL,NULL),(119,61,'Option 2 (Correct)',1,NULL,NULL),(120,61,'Option 3',0,NULL,NULL),(121,62,'Correct Option',1,NULL,NULL),(122,62,'Wrong Option',0,NULL,NULL),(123,63,'A',1,'1',NULL),(124,63,'B',1,'2',NULL),(125,64,'Correct Option',1,NULL,NULL),(126,64,'Wrong Option',0,NULL,NULL),(127,65,'answer',1,NULL,NULL),(128,66,'Correct Option',1,NULL,NULL),(129,66,'Wrong Option',0,NULL,NULL),(130,67,'Option 1 (Correct)',1,NULL,NULL),(131,67,'Option 2 (Correct)',1,NULL,NULL),(132,67,'Option 3',0,NULL,NULL),(133,68,'Option 1 (Correct)',1,NULL,NULL),(134,68,'Option 2 (Correct)',1,NULL,NULL),(135,68,'Option 3',0,NULL,NULL),(136,69,'Correct Option',1,NULL,NULL),(137,69,'Wrong Option',0,NULL,NULL),(138,70,'Correct Option',1,NULL,NULL),(139,70,'Wrong Option',0,NULL,NULL),(140,71,'Correct Option',1,NULL,NULL),(141,71,'Wrong Option',0,NULL,NULL),(142,72,'Correct Option',1,NULL,NULL),(143,72,'Wrong Option',0,NULL,NULL),(144,73,'A',1,'1',NULL),(145,73,'B',1,'2',NULL),(146,74,'Option 1 (Correct)',1,NULL,NULL),(147,74,'Option 2 (Correct)',1,NULL,NULL),(148,74,'Option 3',0,NULL,NULL),(149,75,'answer',1,NULL,NULL),(150,76,'Correct Option',1,NULL,NULL),(151,76,'Wrong Option',0,NULL,NULL),(152,77,'A',1,'1',NULL),(153,77,'B',1,'2',NULL),(154,78,'Option 1 (Correct)',1,NULL,NULL),(155,78,'Option 2 (Correct)',1,NULL,NULL),(156,78,'Option 3',0,NULL,NULL),(157,79,'answer',1,NULL,NULL),(158,80,'Option 1 (Correct)',1,NULL,NULL),(159,80,'Option 2 (Correct)',1,NULL,NULL),(160,80,'Option 3',0,NULL,NULL),(161,81,'Correct Option',1,NULL,NULL),(162,81,'Wrong Option',0,NULL,NULL),(163,82,'Option 1 (Correct)',1,NULL,NULL),(164,82,'Option 2 (Correct)',1,NULL,NULL),(165,82,'Option 3',0,NULL,NULL),(166,83,'answer',1,NULL,NULL),(167,84,'Option 1 (Correct)',1,NULL,NULL),(168,84,'Option 2 (Correct)',1,NULL,NULL),(169,84,'Option 3',0,NULL,NULL),(170,85,'Correct Option',1,NULL,NULL),(171,85,'Wrong Option',0,NULL,NULL),(172,86,'A',1,'1',NULL),(173,86,'B',1,'2',NULL),(174,87,'A',1,'1',NULL),(175,87,'B',1,'2',NULL),(176,88,'Correct Option',1,NULL,NULL),(177,88,'Wrong Option',0,NULL,NULL),(178,89,'Correct Option',1,NULL,NULL),(179,89,'Wrong Option',0,NULL,NULL),(180,90,'Correct Option',1,NULL,NULL),(181,90,'Wrong Option',0,NULL,NULL),(182,91,'Correct Option',1,NULL,NULL),(183,91,'Wrong Option',0,NULL,NULL),(184,92,'A',1,'1',NULL),(185,92,'B',1,'2',NULL),(186,93,'answer',1,NULL,NULL),(187,94,'Correct Option',1,NULL,NULL),(188,94,'Wrong Option',0,NULL,NULL),(189,95,'Correct Option',1,NULL,NULL),(190,95,'Wrong Option',0,NULL,NULL),(191,96,'A',1,'1',NULL),(192,96,'B',1,'2',NULL),(193,97,'Option 1 (Correct)',1,NULL,NULL),(194,97,'Option 2 (Correct)',1,NULL,NULL),(195,97,'Option 3',0,NULL,NULL),(196,98,'A',1,'1',NULL),(197,98,'B',1,'2',NULL),(198,99,'Option 1 (Correct)',1,NULL,NULL),(199,99,'Option 2 (Correct)',1,NULL,NULL),(200,99,'Option 3',0,NULL,NULL),(201,100,'Correct Option',1,NULL,NULL),(202,100,'Wrong Option',0,NULL,NULL),(203,101,'Option 1 (Correct)',1,NULL,NULL),(204,101,'Option 2 (Correct)',1,NULL,NULL),(205,101,'Option 3',0,NULL,NULL),(206,102,'answer',1,NULL,NULL),(207,103,'Option 1 (Correct)',1,NULL,NULL),(208,103,'Option 2 (Correct)',1,NULL,NULL),(209,103,'Option 3',0,NULL,NULL),(210,104,'answer',1,NULL,NULL),(211,105,'Correct Option',1,NULL,NULL),(212,105,'Wrong Option',0,NULL,NULL),(213,106,'Correct Option',1,NULL,NULL),(214,106,'Wrong Option',0,NULL,NULL),(215,107,'A',1,'1',NULL),(216,107,'B',1,'2',NULL),(217,108,'Correct Option',1,NULL,NULL),(218,108,'Wrong Option',0,NULL,NULL),(219,109,'A',1,'1',NULL),(220,109,'B',1,'2',NULL),(221,110,'Correct Option',1,NULL,NULL),(222,110,'Wrong Option',0,NULL,NULL),(223,111,'A',1,'1',NULL),(224,111,'B',1,'2',NULL),(225,112,'Option 1 (Correct)',1,NULL,NULL),(226,112,'Option 2 (Correct)',1,NULL,NULL),(227,112,'Option 3',0,NULL,NULL),(228,113,'answer',1,NULL,NULL),(229,114,'Correct Option',1,NULL,NULL),(230,114,'Wrong Option',0,NULL,NULL),(231,115,'Correct Option',1,NULL,NULL),(232,115,'Wrong Option',0,NULL,NULL),(233,116,'answer',1,NULL,NULL),(234,117,'Correct Option',1,NULL,NULL),(235,117,'Wrong Option',0,NULL,NULL),(236,118,'A',1,'1',NULL),(237,118,'B',1,'2',NULL),(240,120,'Option 1 (Correct)',1,NULL,NULL),(241,120,'Option 2 (Correct)',1,NULL,NULL),(242,120,'Option 3',0,NULL,NULL),(243,119,'A 1',1,'1',0),(244,119,'B 1',0,'2',1);
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
  `meet_link` varchar(255) DEFAULT NULL,
  `number_of_sessions` int DEFAULT NULL,
  PRIMARY KEY (`class_id`),
  KEY `course_id` (`course_id`),
  KEY `teacher_id` (`teacher_id`),
  CONSTRAINT `class_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `class_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=231 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class`
--

LOCK TABLES `class` WRITE;
/*!40000 ALTER TABLE `class` DISABLE KEYS */;
INSERT INTO `class` VALUES (101,101,103,'IELTS-K01','2026-04-01 18:00:00.000000','Open',NULL,NULL,'T2 - T4 - T6 (18:00 - 20:00)',NULL,NULL,NULL),(102,101,103,'IELTS-K02','2026-04-15 19:30:00.000000','Open',NULL,NULL,'T3 - T5 - T7 (19:30 - 21:30)',NULL,NULL,NULL),(103,102,103,'TOEIC-K01','2026-03-20 18:00:00.000000','Closed',NULL,NULL,NULL,NULL,NULL,NULL),(201,205,204,'èiọdioks','2024-03-15 00:00:00.000000','Open','2024-06-15 00:00:00.000000','Thứ 2-4-6','Thứ 3-5-7','18:00 - 20:00',NULL,NULL),(202,201,205,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(203,202,204,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(204,203,205,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(205,201,204,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(206,201,205,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(207,201,205,'I7','2026-03-02 04:13:00.000000',NULL,'2026-03-20 04:13:00.000000',NULL,'Thứ 2-4-6','08:00 - 10:00',NULL,NULL),(208,201,1,NULL,'2026-04-02 04:20:00.000000',NULL,'2026-04-09 04:27:00.000000',NULL,'Thứ 2-4-6','08:00 - 10:00',NULL,NULL),(209,203,1,'aaâ','2026-03-25 14:45:00.000000','Open',NULL,NULL,'Thứ 2-4-6','08:00 - 10:00',NULL,NULL),(210,203,213,'IL157','2026-03-18 14:57:00.000000','Open',NULL,NULL,'Thứ 2-4-6','18:00 - 20:00',NULL,NULL),(211,204,204,'jwio','2026-03-26 08:59:00.000000','Pending',NULL,NULL,'Thứ 2-4-6','08:00 - 10:00',NULL,NULL),(212,206,220,'IELTS-K15','2026-03-23 15:46:29.331973','Open','2026-06-24 15:46:29.331998',NULL,NULL,NULL,NULL,NULL),(213,201,204,'luyện thi','2026-03-24 17:00:00.000000','Open','2026-03-30 17:00:00.000000',NULL,'2,4,6','Chiều (13:00 - 15:00)',NULL,NULL),(217,210,204,'Luyện thi cấp tốc','2026-03-26 17:00:00.000000','Closed','2026-03-30 17:00:00.000000',NULL,'2,4,6','Sáng (9:00 - 11:00)',NULL,30),(220,210,205,'Chơi là thắng','2026-03-25 17:00:00.000000','Open','2026-03-30 17:00:00.000000',NULL,'2,4,6','Chiều (13:00 - 15:00)',NULL,24),(229,207,223,'Lớp Tiếng anh Ielts 5.0 - 6.5','2026-03-24 17:00:00.000000','Pending','2026-04-09 17:00:00.000000',NULL,'Thứ 3, 5, 6','Sáng (9:00 - 11:00)',NULL,30),(230,209,224,'Lớp Tiếng anh Ielts 5.0 - 6.5','2026-03-26 17:00:00.000000','Open','2026-03-27 17:00:00.000000',NULL,'Thứ 3, 5,7','Sáng (7:00 - 9:00)',NULL,30);
/*!40000 ALTER TABLE `class` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_session`
--

DROP TABLE IF EXISTS `class_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_session` (
  `session_id` int NOT NULL AUTO_INCREMENT,
  `end_time` varchar(255) DEFAULT NULL,
  `notes` text,
  `session_date` datetime(6) DEFAULT NULL,
  `session_number` int NOT NULL,
  `start_time` varchar(255) DEFAULT NULL,
  `topic` varchar(255) DEFAULT NULL,
  `class_id` int NOT NULL,
  `quiz_id` int DEFAULT NULL,
  `materials` text,
  PRIMARY KEY (`session_id`),
  KEY `FKsdnp0fq88ihhlbresf40c43u` (`class_id`),
  KEY `FKjhwc3g27oaxwxan7hxgbdb6pv` (`quiz_id`),
  CONSTRAINT `FKjhwc3g27oaxwxan7hxgbdb6pv` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`),
  CONSTRAINT `FKsdnp0fq88ihhlbresf40c43u` FOREIGN KEY (`class_id`) REFERENCES `class` (`class_id`)
) ENGINE=InnoDB AUTO_INCREMENT=111 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_session`
--

LOCK TABLES `class_session` WRITE;
/*!40000 ALTER TABLE `class_session` DISABLE KEYS */;
INSERT INTO `class_session` VALUES (1,'15:00',NULL,'2026-03-26 06:00:00.000000',1,'13:00',NULL,217,NULL,NULL),(2,'15:00',NULL,'2026-03-28 06:00:00.000000',2,'13:00',NULL,217,NULL,NULL),(3,'15:00',NULL,'2026-03-31 06:00:00.000000',3,'13:00',NULL,217,NULL,NULL),(4,'15:00',NULL,'2026-04-02 06:00:00.000000',4,'13:00',NULL,217,NULL,NULL),(5,'15:00',NULL,'2026-04-04 06:00:00.000000',5,'13:00',NULL,217,NULL,NULL),(6,'15:00',NULL,'2026-04-07 06:00:00.000000',6,'13:00',NULL,217,NULL,NULL),(7,'15:00',NULL,'2026-04-09 06:00:00.000000',7,'13:00',NULL,217,NULL,NULL),(8,'15:00',NULL,'2026-04-11 06:00:00.000000',8,'13:00',NULL,217,NULL,NULL),(9,'15:00',NULL,'2026-04-14 06:00:00.000000',9,'13:00',NULL,217,NULL,NULL),(10,'15:00',NULL,'2026-04-16 06:00:00.000000',10,'13:00',NULL,217,NULL,NULL),(11,'15:00',NULL,'2026-04-18 06:00:00.000000',11,'13:00',NULL,217,NULL,NULL),(12,'15:00',NULL,'2026-04-21 06:00:00.000000',12,'13:00',NULL,217,NULL,NULL),(13,'15:00',NULL,'2026-04-23 06:00:00.000000',13,'13:00',NULL,217,NULL,NULL),(14,'15:00',NULL,'2026-04-25 06:00:00.000000',14,'13:00',NULL,217,NULL,NULL),(15,'15:00',NULL,'2026-04-28 06:00:00.000000',15,'13:00',NULL,217,NULL,NULL),(16,'15:00',NULL,'2026-04-30 06:00:00.000000',16,'13:00',NULL,217,NULL,NULL),(17,'15:00',NULL,'2026-05-02 06:00:00.000000',17,'13:00',NULL,217,NULL,NULL),(18,'15:00',NULL,'2026-05-05 06:00:00.000000',18,'13:00',NULL,217,NULL,NULL),(19,'15:00',NULL,'2026-05-07 06:00:00.000000',19,'13:00',NULL,217,NULL,NULL),(20,'15:00',NULL,'2026-05-09 06:00:00.000000',20,'13:00',NULL,217,NULL,NULL),(21,'15:00',NULL,'2026-05-12 06:00:00.000000',21,'13:00',NULL,217,NULL,NULL),(22,'15:00',NULL,'2026-05-14 06:00:00.000000',22,'13:00',NULL,217,NULL,NULL),(23,'15:00',NULL,'2026-05-16 06:00:00.000000',23,'13:00',NULL,217,NULL,NULL),(24,'15:00',NULL,'2026-05-19 06:00:00.000000',24,'13:00',NULL,217,NULL,NULL),(25,'15:00',NULL,'2026-05-21 06:00:00.000000',25,'13:00',NULL,217,NULL,NULL),(26,'15:00',NULL,'2026-05-23 06:00:00.000000',26,'13:00',NULL,217,NULL,NULL),(27,'15:00',NULL,'2026-03-26 06:00:00.000000',1,'13:00',NULL,220,NULL,NULL),(28,'15:00',NULL,'2026-03-28 06:00:00.000000',2,'13:00',NULL,220,NULL,NULL),(29,'15:00',NULL,'2026-03-31 06:00:00.000000',3,'13:00',NULL,220,NULL,NULL),(30,'15:00',NULL,'2026-04-02 06:00:00.000000',4,'13:00',NULL,220,NULL,NULL),(31,'15:00',NULL,'2026-04-04 06:00:00.000000',5,'13:00',NULL,220,NULL,NULL),(32,'15:00',NULL,'2026-04-07 06:00:00.000000',6,'13:00',NULL,220,NULL,NULL),(33,'15:00',NULL,'2026-04-09 06:00:00.000000',7,'13:00',NULL,220,NULL,NULL),(34,'15:00',NULL,'2026-04-11 06:00:00.000000',8,'13:00',NULL,220,NULL,NULL),(35,'15:00',NULL,'2026-04-14 06:00:00.000000',9,'13:00',NULL,220,NULL,NULL),(36,'15:00',NULL,'2026-04-16 06:00:00.000000',10,'13:00',NULL,220,NULL,NULL),(37,'15:00',NULL,'2026-04-18 06:00:00.000000',11,'13:00',NULL,220,NULL,NULL),(38,'15:00',NULL,'2026-04-21 06:00:00.000000',12,'13:00',NULL,220,NULL,NULL),(39,'15:00',NULL,'2026-04-23 06:00:00.000000',13,'13:00',NULL,220,NULL,NULL),(40,'15:00',NULL,'2026-04-25 06:00:00.000000',14,'13:00',NULL,220,NULL,NULL),(41,'15:00',NULL,'2026-04-28 06:00:00.000000',15,'13:00',NULL,220,NULL,NULL),(42,'15:00',NULL,'2026-04-30 06:00:00.000000',16,'13:00',NULL,220,NULL,NULL),(43,'15:00',NULL,'2026-05-02 06:00:00.000000',17,'13:00',NULL,220,NULL,NULL),(44,'15:00',NULL,'2026-05-05 06:00:00.000000',18,'13:00',NULL,220,NULL,NULL),(45,'15:00',NULL,'2026-05-07 06:00:00.000000',19,'13:00',NULL,220,NULL,NULL),(46,'15:00',NULL,'2026-05-09 06:00:00.000000',20,'13:00',NULL,220,NULL,NULL),(47,'15:00',NULL,'2026-05-12 06:00:00.000000',21,'13:00',NULL,220,NULL,NULL),(48,'15:00',NULL,'2026-05-14 06:00:00.000000',22,'13:00',NULL,220,NULL,NULL),(49,'15:00',NULL,'2026-05-16 06:00:00.000000',23,'13:00',NULL,220,NULL,NULL),(50,'15:00',NULL,'2026-05-19 06:00:00.000000',24,'13:00',NULL,220,NULL,NULL),(51,'15:00',NULL,'2026-03-25 06:00:00.000000',1,'13:00',NULL,229,NULL,NULL),(52,'15:00',NULL,'2026-03-27 06:00:00.000000',2,'13:00',NULL,229,NULL,NULL),(53,'15:00',NULL,'2026-03-28 06:00:00.000000',3,'13:00',NULL,229,NULL,NULL),(54,'15:00',NULL,'2026-04-01 06:00:00.000000',4,'13:00',NULL,229,NULL,NULL),(55,'15:00',NULL,'2026-04-03 06:00:00.000000',5,'13:00',NULL,229,NULL,NULL),(56,'15:00',NULL,'2026-04-04 06:00:00.000000',6,'13:00',NULL,229,NULL,NULL),(57,'15:00',NULL,'2026-04-08 06:00:00.000000',7,'13:00',NULL,229,NULL,NULL),(58,'15:00',NULL,'2026-04-10 06:00:00.000000',8,'13:00',NULL,229,NULL,NULL),(59,'15:00',NULL,'2026-04-11 06:00:00.000000',9,'13:00',NULL,229,NULL,NULL),(60,'15:00',NULL,'2026-04-15 06:00:00.000000',10,'13:00',NULL,229,NULL,NULL),(61,'15:00',NULL,'2026-04-17 06:00:00.000000',11,'13:00',NULL,229,NULL,NULL),(62,'15:00',NULL,'2026-04-18 06:00:00.000000',12,'13:00',NULL,229,NULL,NULL),(63,'15:00',NULL,'2026-04-22 06:00:00.000000',13,'13:00',NULL,229,NULL,NULL),(64,'15:00',NULL,'2026-04-24 06:00:00.000000',14,'13:00',NULL,229,NULL,NULL),(65,'15:00',NULL,'2026-04-25 06:00:00.000000',15,'13:00',NULL,229,NULL,NULL),(66,'15:00',NULL,'2026-04-29 06:00:00.000000',16,'13:00',NULL,229,NULL,NULL),(67,'15:00',NULL,'2026-05-01 06:00:00.000000',17,'13:00',NULL,229,NULL,NULL),(68,'15:00',NULL,'2026-05-02 06:00:00.000000',18,'13:00',NULL,229,NULL,NULL),(69,'15:00',NULL,'2026-05-06 06:00:00.000000',19,'13:00',NULL,229,NULL,NULL),(70,'15:00',NULL,'2026-05-08 06:00:00.000000',20,'13:00',NULL,229,NULL,NULL),(71,'15:00',NULL,'2026-05-09 06:00:00.000000',21,'13:00',NULL,229,NULL,NULL),(72,'15:00',NULL,'2026-05-13 06:00:00.000000',22,'13:00',NULL,229,NULL,NULL),(73,'15:00',NULL,'2026-05-15 06:00:00.000000',23,'13:00',NULL,229,NULL,NULL),(74,'15:00',NULL,'2026-05-16 06:00:00.000000',24,'13:00',NULL,229,NULL,NULL),(75,'15:00',NULL,'2026-05-20 06:00:00.000000',25,'13:00',NULL,229,NULL,NULL),(76,'15:00',NULL,'2026-05-22 06:00:00.000000',26,'13:00',NULL,229,NULL,NULL),(77,'15:00',NULL,'2026-05-23 06:00:00.000000',27,'13:00',NULL,229,NULL,NULL),(78,'15:00',NULL,'2026-05-27 06:00:00.000000',28,'13:00',NULL,229,NULL,NULL),(79,'15:00',NULL,'2026-05-29 06:00:00.000000',29,'13:00',NULL,229,NULL,NULL),(80,'15:00',NULL,'2026-05-30 06:00:00.000000',30,'13:00',NULL,229,NULL,NULL),(81,'09:00',NULL,'2026-03-27 00:00:00.000000',1,'07:00',NULL,230,NULL,NULL),(82,'09:00',NULL,'2026-03-29 00:00:00.000000',2,'07:00',NULL,230,NULL,NULL),(83,'09:00',NULL,'2026-04-01 00:00:00.000000',3,'07:00',NULL,230,NULL,NULL),(84,'09:00',NULL,'2026-04-03 00:00:00.000000',4,'07:00',NULL,230,NULL,NULL),(85,'09:00',NULL,'2026-04-05 00:00:00.000000',5,'07:00',NULL,230,NULL,NULL),(86,'09:00',NULL,'2026-04-08 00:00:00.000000',6,'07:00',NULL,230,NULL,NULL),(87,'09:00',NULL,'2026-04-10 00:00:00.000000',7,'07:00',NULL,230,NULL,NULL),(88,'09:00',NULL,'2026-04-12 00:00:00.000000',8,'07:00',NULL,230,NULL,NULL),(89,'09:00',NULL,'2026-04-15 00:00:00.000000',9,'07:00',NULL,230,NULL,NULL),(90,'09:00',NULL,'2026-04-17 00:00:00.000000',10,'07:00',NULL,230,NULL,NULL),(91,'09:00',NULL,'2026-04-19 00:00:00.000000',11,'07:00',NULL,230,NULL,NULL),(92,'09:00',NULL,'2026-04-22 00:00:00.000000',12,'07:00',NULL,230,NULL,NULL),(93,'09:00',NULL,'2026-04-24 00:00:00.000000',13,'07:00',NULL,230,NULL,NULL),(94,'09:00',NULL,'2026-04-26 00:00:00.000000',14,'07:00',NULL,230,NULL,NULL),(95,'09:00',NULL,'2026-04-29 00:00:00.000000',15,'07:00',NULL,230,NULL,NULL),(96,'09:00',NULL,'2026-05-01 00:00:00.000000',16,'07:00',NULL,230,NULL,NULL),(97,'09:00',NULL,'2026-05-03 00:00:00.000000',17,'07:00',NULL,230,NULL,NULL),(98,'09:00',NULL,'2026-05-06 00:00:00.000000',18,'07:00',NULL,230,NULL,NULL),(99,'09:00',NULL,'2026-05-08 00:00:00.000000',19,'07:00',NULL,230,NULL,NULL),(100,'09:00',NULL,'2026-05-10 00:00:00.000000',20,'07:00',NULL,230,NULL,NULL),(101,'09:00',NULL,'2026-05-13 00:00:00.000000',21,'07:00',NULL,230,NULL,NULL),(102,'09:00',NULL,'2026-05-15 00:00:00.000000',22,'07:00',NULL,230,NULL,NULL),(103,'09:00',NULL,'2026-05-17 00:00:00.000000',23,'07:00',NULL,230,NULL,NULL),(104,'09:00',NULL,'2026-05-20 00:00:00.000000',24,'07:00',NULL,230,NULL,NULL),(105,'09:00',NULL,'2026-05-22 00:00:00.000000',25,'07:00',NULL,230,NULL,NULL),(106,'09:00',NULL,'2026-05-24 00:00:00.000000',26,'07:00',NULL,230,NULL,NULL),(107,'09:00',NULL,'2026-05-27 00:00:00.000000',27,'07:00',NULL,230,NULL,NULL),(108,'09:00',NULL,'2026-05-29 00:00:00.000000',28,'07:00',NULL,230,NULL,NULL),(109,'09:00',NULL,'2026-05-31 00:00:00.000000',29,'07:00',NULL,230,NULL,NULL),(110,'09:00',NULL,'2026-06-03 00:00:00.000000',30,'07:00',NULL,230,NULL,NULL);
/*!40000 ALTER TABLE `class_session` ENABLE KEYS */;
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
  `avatar` varchar(255) DEFAULT NULL,
  `level_tag` varchar(10) DEFAULT NULL,
  `sale` double DEFAULT NULL,
  PRIMARY KEY (`course_id`),
  KEY `category_id` (`category_id`),
  KEY `expert_id` (`expert_id`),
  KEY `manager_id` (`manager_id`),
  CONSTRAINT `course_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `setting` (`setting_id`),
  CONSTRAINT `course_ibfk_2` FOREIGN KEY (`expert_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `course_ibfk_3` FOREIGN KEY (`manager_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course`
--

LOCK TABLES `course` WRITE;
/*!40000 ALTER TABLE `course` DISABLE KEYS */;
INSERT INTO `course` VALUES (101,NULL,101,102,101,'Active','Khóa học nền tảng IELTS 5.0','/assets/img/education/courses-13.webp',6000000,'IELTS Foundation',NULL,NULL,NULL),(102,'IELTS 8.0',102,203,101,'active','Chinh phục TOEIC 500+ trong 2 tháng','/assets/img/education/students-9.webp',4000000,'IELTS 8.0','youtube.com/?themeRefresh=1',NULL,0),(201,'Luyện thi IELTS Academic',206,203,202,'ACTIVE',NULL,NULL,NULL,NULL,NULL,NULL,NULL),(202,'Luyện thi TOEIC 2 Kỹ năng',207,203,202,'ACTIVE',NULL,NULL,NULL,NULL,NULL,NULL,NULL),(203,'Tiếng Anh Giao tiếp Phản xạ',208,203,202,'ACTIVE',NULL,NULL,NULL,NULL,NULL,NULL,NULL),(204,'topic123456',NULL,NULL,NULL,'active','',NULL,500000,'sl',NULL,NULL,NULL),(205,'Msklưenakl',NULL,NULL,NULL,'active','bnvshjacvkjhsagvdhgdjhghhksjfdhjl',NULL,5000000,'IL258',NULL,NULL,NULL),(206,'IELTS 6.5+ Intensive 4 Skills',213,220,NULL,'Published','Luyện thi IELTS chuyên sâu, master Listening, Reading, Writing, Speaking.',NULL,1500000,'Khóa học IELTS Toàn Diện 4 Kỹ Năng',NULL,NULL,NULL),(207,'Course Title 1',214,222,NULL,'Published','Comprehensive course for leveling up your skills.',NULL,1000000,'Mastering English 1',NULL,'B1',NULL),(208,'Course Title 2',214,221,NULL,'Published','Comprehensive course for leveling up your skills.',NULL,2000000,'Mastering English 2',NULL,'C1',NULL),(209,'Course Title 3',214,222,NULL,'Published','Comprehensive course for leveling up your skills.',NULL,3000000,'Mastering English 3',NULL,'C1',NULL),(210,'IELTS 4.0',206,203,NULL,'active','Học chăm chỉ',NULL,10000,'IELTS 4.0','https://www.pinterest.com/cachhaynhat/900%2B-h%C3%ACnh-%E1%BA%A3nh-m%C3%A8o-cute-ho%E1%BA%A1t-h%C3%ACnh-ng%E1%BA%A7u-bu%E1%BB%93n/',NULL,0);
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
) ENGINE=InnoDB AUTO_INCREMENT=260 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lesson`
--

LOCK TABLES `lesson` WRITE;
/*!40000 ALTER TABLE `lesson` DISABLE KEYS */;
INSERT INTO `lesson` VALUES (1,1,1,NULL,'15:30','Bài 1: Tổng quan về IELTS Listening',NULL,'VIDEO','https://youtube.com/embed/xyz123'),(2,1,2,NULL,'20:00','Bài 2: Kỹ năng bắt Keywords',NULL,'VIDEO','https://youtube.com/embed/abc456'),(3,1,3,'Nội dung chi tiết tài liệu học tập môn Nghe...','5 Trang','Tài liệu: Các bẫy thường gặp trong Section 1',NULL,'DOC',NULL),(4,1,4,NULL,'10 Câu hỏi','Quiz: Test Listening Section 1',NULL,'QUIZ',NULL),(5,2,1,NULL,'18:45','Bài 1: Kỹ năng Skimming & Scanning',NULL,'VIDEO','https://youtube.com/embed/def789'),(6,2,2,'Đoạn văn bài tập Reading...','Bài tập thực hành','Bài tập: Luyện đọc hiểu True/False/Not Given',NULL,'DOC',NULL),(7,4,1,NULL,'12:00','Cách làm dạng bài tranh tả người',NULL,'VIDEO','https://youtube.com/embed/toeic123'),(201,201,1,NULL,NULL,NULL,NULL,NULL,NULL),(202,201,2,NULL,NULL,NULL,NULL,NULL,NULL),(203,202,1,NULL,NULL,NULL,NULL,NULL,NULL),(204,203,1,NULL,'15:00','Chiến thuật Skimming & Scanning',NULL,'VIDEO',NULL),(205,203,2,NULL,'20:00','IELTS Reading Mini-Test 1',202,'QUIZ',NULL),(206,204,1,NULL,NULL,'Lesson 1 - Module 1 of Course 1',NULL,'READING',NULL),(207,204,2,NULL,NULL,'Lesson 2 - Module 1 of Course 1',NULL,'VIDEO',NULL),(208,204,3,NULL,NULL,'Lesson 3 - Module 1 of Course 1',NULL,'READING',NULL),(209,204,4,NULL,NULL,'Lesson 4 - Module 1 of Course 1',NULL,'VIDEO',NULL),(210,205,1,NULL,NULL,'Lesson 1 - Module 2 of Course 1',NULL,'READING',NULL),(211,205,2,NULL,NULL,'Lesson 2 - Module 2 of Course 1',NULL,'VIDEO',NULL),(212,205,3,NULL,NULL,'Lesson 3 - Module 2 of Course 1',NULL,'READING',NULL),(213,205,4,NULL,NULL,'Lesson 4 - Module 2 of Course 1',NULL,'VIDEO',NULL),(214,206,1,NULL,NULL,'Lesson 1 - Module 3 of Course 1',NULL,'READING',NULL),(215,206,2,NULL,NULL,'Lesson 2 - Module 3 of Course 1',NULL,'VIDEO',NULL),(216,206,3,NULL,NULL,'Lesson 3 - Module 3 of Course 1',NULL,'READING',NULL),(217,206,4,NULL,NULL,'Lesson 4 - Module 3 of Course 1',NULL,'VIDEO',NULL),(218,207,1,NULL,NULL,'Lesson 1 - Module 4 of Course 1',NULL,'READING',NULL),(219,207,2,NULL,NULL,'Lesson 2 - Module 4 of Course 1',NULL,'VIDEO',NULL),(220,207,3,NULL,NULL,'Lesson 3 - Module 4 of Course 1',NULL,'READING',NULL),(221,207,4,NULL,NULL,'Lesson 4 - Module 4 of Course 1',NULL,'VIDEO',NULL),(222,208,1,NULL,NULL,'Lesson 1 - Module 1 of Course 2',NULL,'READING',NULL),(223,208,2,NULL,NULL,'Lesson 2 - Module 1 of Course 2',NULL,'VIDEO',NULL),(224,208,3,NULL,NULL,'Lesson 3 - Module 1 of Course 2',NULL,'READING',NULL),(225,208,4,NULL,NULL,'Lesson 4 - Module 1 of Course 2',NULL,'VIDEO',NULL),(226,209,1,NULL,NULL,'Lesson 1 - Module 2 of Course 2',NULL,'READING',NULL),(227,209,2,NULL,NULL,'Lesson 2 - Module 2 of Course 2',NULL,'VIDEO',NULL),(228,209,3,NULL,NULL,'Lesson 3 - Module 2 of Course 2',NULL,'READING',NULL),(229,209,4,NULL,NULL,'Lesson 4 - Module 2 of Course 2',NULL,'VIDEO',NULL),(230,210,1,NULL,NULL,'Lesson 1 - Module 3 of Course 2',NULL,'READING',NULL),(231,210,2,NULL,NULL,'Lesson 2 - Module 3 of Course 2',NULL,'VIDEO',NULL),(232,210,3,NULL,NULL,'Lesson 3 - Module 3 of Course 2',NULL,'READING',NULL),(233,210,4,NULL,NULL,'Lesson 4 - Module 3 of Course 2',NULL,'VIDEO',NULL),(234,211,1,NULL,NULL,'Lesson 1 - Module 4 of Course 2',NULL,'READING',NULL),(235,211,2,NULL,NULL,'Lesson 2 - Module 4 of Course 2',NULL,'VIDEO',NULL),(236,211,3,NULL,NULL,'Lesson 3 - Module 4 of Course 2',NULL,'READING',NULL),(237,211,4,NULL,NULL,'Lesson 4 - Module 4 of Course 2',NULL,'VIDEO',NULL),(238,212,1,NULL,NULL,'Lesson 1 - Module 1 of Course 3',NULL,'READING',NULL),(239,212,2,NULL,NULL,'Lesson 2 - Module 1 of Course 3',NULL,'VIDEO',NULL),(240,212,3,NULL,NULL,'Lesson 3 - Module 1 of Course 3',NULL,'READING',NULL),(241,212,4,NULL,NULL,'Lesson 4 - Module 1 of Course 3',NULL,'VIDEO',NULL),(242,213,1,NULL,NULL,'Lesson 1 - Module 2 of Course 3',NULL,'READING',NULL),(243,213,2,NULL,NULL,'Lesson 2 - Module 2 of Course 3',NULL,'VIDEO',NULL),(244,213,3,NULL,NULL,'Lesson 3 - Module 2 of Course 3',NULL,'READING',NULL),(245,213,4,NULL,NULL,'Lesson 4 - Module 2 of Course 3',NULL,'VIDEO',NULL),(246,214,1,NULL,NULL,'Lesson 1 - Module 3 of Course 3',NULL,'READING',NULL),(247,214,2,NULL,NULL,'Lesson 2 - Module 3 of Course 3',NULL,'VIDEO',NULL),(248,214,3,NULL,NULL,'Lesson 3 - Module 3 of Course 3',NULL,'READING',NULL),(249,214,4,NULL,NULL,'Lesson 4 - Module 3 of Course 3',NULL,'VIDEO',NULL),(250,215,1,NULL,NULL,'Lesson 1 - Module 4 of Course 3',NULL,'READING',NULL),(251,215,2,NULL,NULL,'Lesson 2 - Module 4 of Course 3',NULL,'VIDEO',NULL),(252,215,3,NULL,NULL,'Lesson 3 - Module 4 of Course 3',NULL,'READING',NULL),(253,215,4,NULL,NULL,'Lesson 4 - Module 4 of Course 3',NULL,'VIDEO',NULL),(254,208,2,NULL,NULL,'quiz 1',207,'QUIZ',NULL),(255,4,2,NULL,NULL,'quiz 3',207,'QUIZ',NULL),(256,216,1,NULL,NULL,'mèo méo meo',NULL,'VIDEO','https://www.youtube.com/shorts/lVWluOnH9UE'),(257,217,1,NULL,NULL,'nà ná na na',NULL,'VIDEO','https://www.youtube.com/watch?v=PD61lIYrG-M&list=RDPD61lIYrG-M&start_radio=1'),(258,219,1,'<p>Từ vựng mới</p><p><br></p><p><br></p>',NULL,'Bài học 1',NULL,'DOC',NULL),(259,217,2,'<p>sda</p>',NULL,'chương 1',NULL,'DOC',NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=220 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `module`
--

LOCK TABLES `module` WRITE;
/*!40000 ALTER TABLE `module` DISABLE KEYS */;
INSERT INTO `module` VALUES (1,101,1,'Chương 1: Listening Foundation (Nền tảng Nghe)'),(2,101,2,'Chương 2: Reading Foundation (Nền tảng Đọc)'),(3,101,3,'Chương 3: Speaking & Writing Basics'),(4,102,1,'Chương 1: Part 1 - Photographs'),(201,201,1,NULL),(202,201,2,NULL),(203,206,1,'Chương 1: Reading Fundamentals'),(204,207,1,'Module 1 of Course 1'),(205,207,2,'Module 2 of Course 1'),(206,207,3,'Module 3 of Course 1'),(207,207,4,'Module 4 of Course 1'),(208,208,1,'Module 1 of Course 2'),(209,208,2,'Module 2 of Course 2'),(210,208,3,'Module 3 of Course 2'),(211,208,4,'Module 4 of Course 2'),(212,209,1,'Module 1 of Course 3'),(213,209,2,'Module 2 of Course 3'),(214,209,3,'Module 3 of Course 3'),(215,209,4,'Module 4 of Course 3'),(216,210,1,'Chương 1: Học cho vui'),(217,210,2,'Chương 2: Ngủ thì thích'),(218,210,3,'Chương 3 : Chơi hết mình'),(219,102,2,'Chương 1 : Part 2 - Dat');
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
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `amount` decimal(38,2) DEFAULT NULL,
  `checkout_url` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payos_order_code` bigint NOT NULL,
  `payos_payment_link_id` bigint DEFAULT NULL,
  `qr_code` varchar(500) DEFAULT NULL,
  `registration_id` int NOT NULL,
  `status` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKunfhr5os47gfge6eq19tmkf4` (`payos_order_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
INSERT INTO `payment` VALUES (1,5000000.00,'https://pay.payos.vn/web/f56e80278a1e4d89a921c17fadb6f62d','2026-03-25 08:57:23.701438','Msklưenakl',NULL,43701,NULL,'00020101021238570010A000000727012700069704220113VQRQAHVUW59080208QRIBFTTA5303704540750000005802VN62140810Mskluenakl630447EB',207,'PENDING'),(2,10000.00,'https://pay.payos.vn/web/194da731a0f34f23a4a4947aade86e29','2026-03-25 15:01:35.239423','IELTS 4.0','2026-03-25 15:02:18.397167',895237,NULL,'00020101021238570010A000000727012700069704220113VQRQAHWBL70780208QRIBFTTA53037045405100005802VN62120808IELTS 406304F019',209,'PAID'),(3,10000.00,'https://pay.payos.vn/web/a7302361857245f689853ed8f84ee4a9','2026-03-25 15:31:45.732491','IELTS 4.0','2026-03-25 15:32:18.241501',705732,NULL,'00020101021238570010A000000727012700069704220113VQRQAHWBQ85140208QRIBFTTA53037045405100005802VN62120808IELTS 406304B9AA',210,'PAID'),(4,5000000.00,'https://pay.payos.vn/web/d75e144dd31242228d7bc6a6c1b4ddf9','2026-03-25 16:15:50.677397','Msklưenakl',NULL,350677,NULL,'00020101021238570010A000000727012700069704220113VQRQAHWBV26940208QRIBFTTA5303704540750000005802VN62140810Mskluenakl6304DFEE',213,'CANCELLED'),(5,5000000.00,'https://pay.payos.vn/web/a3c3707436194cf7991d8841de1cd149','2026-03-25 16:16:12.770277','Msklưenakl',NULL,372769,NULL,'00020101021238570010A000000727012700069704220113VQRQAHWBV28420208QRIBFTTA5303704540750000005802VN62140810Mskluenakl6304E4E6',213,'CANCELLED'),(6,10000.00,'https://pay.payos.vn/web/73b32e25d56a452aab45f98c378332a7','2026-03-26 02:24:16.970112','IELTS 4.0',NULL,856970,NULL,'00020101021238570010A000000727012700069704220113VQRQAHWEK33820208QRIBFTTA53037045405100005802VN62120808IELTS 406304EF97',214,'CANCELLED'),(7,10000.00,'https://pay.payos.vn/web/fbce4a67c86041378712e9eefe7a2537','2026-03-27 09:37:51.571482','IELTS 4.0',NULL,271557,NULL,'00020101021238570010A000000727012700069704220113VQRQAHWYB17060208QRIBFTTA53037045405100005802VN62120808IELTS 406304A951',216,'PENDING'),(8,10000.00,'https://pay.payos.vn/web/12be76b4f94c4712a1ff705efb092492','2026-03-27 09:37:52.638828','IELTS 4.0',NULL,272638,NULL,'00020101021238570010A000000727012700069704220113VQRQAHWYB17140208QRIBFTTA53037045405100005802VN62120808IELTS 406304A2FC',217,'PENDING');
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `placement_test_answer`
--

DROP TABLE IF EXISTS `placement_test_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `placement_test_answer` (
  `id` int NOT NULL AUTO_INCREMENT,
  `answered_options` text,
  `is_correct` bit(1) DEFAULT NULL,
  `placement_result_id` int NOT NULL,
  `question_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlyxkciqwp8ffh26lntkqvxxu1` (`placement_result_id`),
  KEY `FKkuovghh1jb6uqaoxpy94vuu97` (`question_id`),
  CONSTRAINT `FKkuovghh1jb6uqaoxpy94vuu97` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`),
  CONSTRAINT `FKlyxkciqwp8ffh26lntkqvxxu1` FOREIGN KEY (`placement_result_id`) REFERENCES `placement_test_result` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `placement_test_answer`
--

LOCK TABLES `placement_test_answer` WRITE;
/*!40000 ALTER TABLE `placement_test_answer` DISABLE KEYS */;
INSERT INTO `placement_test_answer` VALUES (1,'{\"223\":\"2\",\"224\":\"1\"}',_binary '\0',1,111),(2,'227',_binary '\0',1,112),(3,'230',_binary '\0',1,114),(4,'232',_binary '\0',1,115),(5,'235',_binary '\0',1,117),(6,'{\"238\":\"1\",\"239\":\"2\"}',_binary '',1,119),(7,'154',_binary '\0',2,78),(8,'235',_binary '\0',2,117),(9,'{\"236\":\"1\",\"237\":\"1\"}',_binary '\0',2,118),(10,'{\"238\":\"2\",\"239\":\"1\"}',_binary '\0',2,119),(11,'240',_binary '\0',2,120),(12,'231',_binary '',2,115),(13,'',_binary '\0',2,114),(14,'225',_binary '\0',2,112),(15,'{\"223\":\"2\",\"224\":\"1\"}',_binary '\0',2,111),(16,'{\"223\":\"2\",\"224\":\"1\"}',_binary '\0',3,111),(17,'225',_binary '\0',3,112),(18,'230',_binary '\0',3,114),(19,'231',_binary '',3,115),(20,'234',_binary '',3,117),(21,'{\"238\":\"2\",\"239\":\"1\"}',_binary '\0',3,119),(22,'',_binary '\0',4,78),(23,'',_binary '\0',4,117),(24,'{\"236\":\"\",\"237\":\"\"}',_binary '\0',4,118),(25,'',_binary '\0',4,119),(26,'',_binary '\0',4,120),(27,'',_binary '\0',4,115),(28,'',_binary '\0',4,114),(29,'',_binary '\0',4,112),(30,'{\"223\":\"\",\"224\":\"\"}',_binary '\0',4,111),(31,'',_binary '\0',5,120),(32,'',_binary '\0',5,119),(33,'{\"236\":\"\",\"237\":\"\"}',_binary '\0',5,118),(34,'',_binary '\0',5,117),(35,'',_binary '\0',5,115),(36,'',_binary '\0',5,114);
/*!40000 ALTER TABLE `placement_test_answer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `placement_test_result`
--

DROP TABLE IF EXISTS `placement_test_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `placement_test_result` (
  `id` int NOT NULL AUTO_INCREMENT,
  `correct_rate` decimal(5,2) DEFAULT NULL,
  `guest_email` varchar(100) DEFAULT NULL,
  `guest_name` varchar(100) DEFAULT NULL,
  `guest_session_id` varchar(100) DEFAULT NULL,
  `passed` bit(1) DEFAULT NULL,
  `score` int DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `suggested_level` varchar(10) DEFAULT NULL,
  `quiz_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb8ininxrg1kii1o1qgxix93dy` (`quiz_id`),
  CONSTRAINT `FKb8ininxrg1kii1o1qgxix93dy` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `placement_test_result`
--

LOCK TABLES `placement_test_result` WRITE;
/*!40000 ALTER TABLE `placement_test_result` DISABLE KEYS */;
INSERT INTO `placement_test_result` VALUES (1,16.67,'ngocdat2003xxx@gmail.com','dog anh','F173BF28BAC3E3AE146CA77DF0692611',_binary '\0',1,'2026-03-24 17:13:36.696170','A1',209),(2,11.11,'','','AFEF54B65B63E71E64674046FCD983B4',_binary '\0',1,'2026-03-25 09:44:43.770074','A1',210),(3,33.33,'','','58B04ED69E92F10BD7AB7BEFF9497E5D',_binary '\0',2,'2026-03-25 14:13:58.257916','A2',209),(4,0.00,'','','9251F3B9EB35DF5F5E695D509E43648F',_binary '\0',0,'2026-03-27 04:47:33.496329','A1',210),(5,0.00,'','','3ADC85D17C04D5B8EB369072F86032E3',_binary '\0',0,'2026-03-27 14:41:46.590028','A1',214);
/*!40000 ALTER TABLE `placement_test_result` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question`
--

DROP TABLE IF EXISTS `question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question` (
  `question_id` int NOT NULL AUTO_INCREMENT,
  `module_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `audio_url` varchar(500) DEFAULT NULL,
  `cefr_level` varchar(5) NOT NULL,
  `content` text,
  `created_at` datetime(6) DEFAULT NULL,
  `explanation` text,
  `image_url` varchar(500) DEFAULT NULL,
  `question_type` varchar(30) NOT NULL,
  `skill` varchar(20) NOT NULL,
  `tags` varchar(500) DEFAULT NULL,
  `topic` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `source` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`question_id`),
  KEY `module_id` (`module_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `question_ibfk_1` FOREIGN KEY (`module_id`) REFERENCES `module` (`module_id`),
  CONSTRAINT `question_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question`
--

LOCK TABLES `question` WRITE;
/*!40000 ALTER TABLE `question` DISABLE KEYS */;
INSERT INTO `question` VALUES (1,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 1 - Type: MULTIPLE_CHOICE_SINGLE (IELTS Section)','2026-03-24 15:46:29.395674',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','Listening',NULL,NULL,'2026-03-24 15:46:29.395674',NULL),(2,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 2 - Type: MATCHING (IELTS Section)','2026-03-24 15:46:29.421007',NULL,NULL,'MATCHING','Listening',NULL,NULL,'2026-03-24 15:46:29.421007',NULL),(3,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 3 - Type: FILL_IN_BLANK (IELTS Section)','2026-03-24 15:46:29.439592',NULL,NULL,'FILL_IN_BLANK','Reading',NULL,NULL,'2026-03-24 15:46:29.439592',NULL),(4,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 4 - Type: MULTIPLE_CHOICE_MULTI (IELTS Section)','2026-03-24 15:46:29.455015',NULL,NULL,'MULTIPLE_CHOICE_MULTI','Reading',NULL,NULL,'2026-03-24 15:46:29.455015',NULL),(5,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 5 - Type: FILL_IN_BLANK (IELTS Section)','2026-03-24 15:46:29.476481',NULL,NULL,'FILL_IN_BLANK','Reading',NULL,NULL,'2026-03-24 15:46:29.476481',NULL),(6,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 6 - Type: MULTIPLE_CHOICE_SINGLE (IELTS Section)','2026-03-24 15:46:29.494903',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','Reading',NULL,NULL,'2026-03-24 15:46:29.494903',NULL),(7,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 7 - Type: MULTIPLE_CHOICE_MULTI (IELTS Section)','2026-03-24 15:46:29.531406',NULL,NULL,'MULTIPLE_CHOICE_MULTI','Listening',NULL,NULL,'2026-03-24 15:46:29.531406',NULL),(8,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 8 - Type: WRITING (IELTS Section)','2026-03-24 15:46:29.558001',NULL,NULL,'WRITING','Writing',NULL,NULL,'2026-03-24 15:46:29.558001',NULL),(9,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 9 - Type: FILL_IN_BLANK (IELTS Section)','2026-03-24 15:46:29.576073',NULL,NULL,'FILL_IN_BLANK','Reading',NULL,NULL,'2026-03-24 15:46:29.576073',NULL),(10,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 10 - Type: WRITING (IELTS Section)','2026-03-24 15:46:29.593400',NULL,NULL,'WRITING','Writing',NULL,NULL,'2026-03-24 15:46:29.593400',NULL),(11,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 11 - Type: FILL_IN_BLANK (IELTS Section)','2026-03-24 15:46:29.621622',NULL,NULL,'FILL_IN_BLANK','Listening',NULL,NULL,'2026-03-24 15:46:29.621622',NULL),(12,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 12 - Type: MULTIPLE_CHOICE_SINGLE (IELTS Section)','2026-03-24 15:46:29.635586',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','Listening',NULL,NULL,'2026-03-24 15:46:29.635586',NULL),(13,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 13 - Type: SPEAKING (IELTS Section)','2026-03-24 15:46:29.658649',NULL,NULL,'SPEAKING','Speaking',NULL,NULL,'2026-03-24 15:46:29.658649',NULL),(14,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 14 - Type: SPEAKING (IELTS Section)','2026-03-24 15:46:29.668687',NULL,NULL,'SPEAKING','Speaking',NULL,NULL,'2026-03-24 15:46:29.668687',NULL),(15,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 15 - Type: MULTIPLE_CHOICE_SINGLE (IELTS Section)','2026-03-24 15:46:29.680483',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','Listening',NULL,NULL,'2026-03-24 15:46:29.680483',NULL),(16,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 16 - Type: MULTIPLE_CHOICE_MULTI (IELTS Section)','2026-03-24 15:46:29.700458',NULL,NULL,'MULTIPLE_CHOICE_MULTI','Listening',NULL,NULL,'2026-03-24 15:46:29.700458',NULL),(17,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 17 - Type: MULTIPLE_CHOICE_SINGLE (IELTS Section)','2026-03-24 15:46:29.726195',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','Listening',NULL,NULL,'2026-03-24 15:46:29.726195',NULL),(18,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 18 - Type: WRITING (IELTS Section)','2026-03-24 15:46:29.763130',NULL,NULL,'WRITING','Writing',NULL,NULL,'2026-03-24 15:46:29.763130',NULL),(19,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 19 - Type: FILL_IN_BLANK (IELTS Section)','2026-03-24 15:46:29.781291',NULL,NULL,'FILL_IN_BLANK','Listening',NULL,NULL,'2026-03-24 15:46:29.781291',NULL),(20,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 20 - Type: MULTIPLE_CHOICE_MULTI (IELTS Section)','2026-03-24 15:46:29.795827',NULL,NULL,'MULTIPLE_CHOICE_MULTI','Reading',NULL,NULL,'2026-03-24 15:46:29.795827',NULL),(21,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 21 - Type: FILL_IN_BLANK (IELTS Section)','2026-03-24 15:46:29.848490',NULL,NULL,'FILL_IN_BLANK','Listening',NULL,NULL,'2026-03-24 15:46:29.848490',NULL),(22,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 22 - Type: MATCHING (IELTS Section)','2026-03-24 15:46:29.862731',NULL,NULL,'MATCHING','Listening',NULL,NULL,'2026-03-24 15:46:29.862731',NULL),(23,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 23 - Type: MATCHING (IELTS Section)','2026-03-24 15:46:29.888618',NULL,NULL,'MATCHING','Reading',NULL,NULL,'2026-03-24 15:46:29.888618',NULL),(24,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 24 - Type: FILL_IN_BLANK (IELTS Section)','2026-03-24 15:46:29.904335',NULL,NULL,'FILL_IN_BLANK','Listening',NULL,NULL,'2026-03-24 15:46:29.904335',NULL),(25,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 25 - Type: SPEAKING (IELTS Section)','2026-03-24 15:46:29.928117',NULL,NULL,'SPEAKING','Speaking',NULL,NULL,'2026-03-24 15:46:29.928117',NULL),(26,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 26 - Type: FILL_IN_BLANK (IELTS Section)','2026-03-24 15:46:29.953491',NULL,NULL,'FILL_IN_BLANK','Reading',NULL,NULL,'2026-03-24 15:46:29.953491',NULL),(27,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 27 - Type: MULTIPLE_CHOICE_MULTI (IELTS Section)','2026-03-24 15:46:29.972482',NULL,NULL,'MULTIPLE_CHOICE_MULTI','Reading',NULL,NULL,'2026-03-24 15:46:29.972482',NULL),(28,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 28 - Type: WRITING (IELTS Section)','2026-03-24 15:46:30.009176',NULL,NULL,'WRITING','Writing',NULL,NULL,'2026-03-24 15:46:30.009176',NULL),(29,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 29 - Type: SPEAKING (IELTS Section)','2026-03-24 15:46:30.032555',NULL,NULL,'SPEAKING','Speaking',NULL,NULL,'2026-03-24 15:46:30.032555',NULL),(30,NULL,NULL,'PUBLISHED',NULL,'C1','English Practice Question 30 - Type: SPEAKING (IELTS Section)','2026-03-24 15:46:30.046726',NULL,NULL,'SPEAKING','Speaking',NULL,NULL,'2026-03-24 15:46:30.046726',NULL),(31,NULL,NULL,'PUBLISHED',NULL,'B1','Question 1 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:30.742261',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:30.742261',NULL),(32,NULL,NULL,'PUBLISHED',NULL,'B1','Question 2 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:30.753227',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:30.753227',NULL),(33,NULL,NULL,'PUBLISHED',NULL,'B1','Question 3 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:30.763806',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:30.763806',NULL),(34,NULL,NULL,'PUBLISHED',NULL,'B1','Question 4 for Quiz 1 (Type: MATCHING)','2026-03-24 15:46:30.782597',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:30.782597',NULL),(35,NULL,NULL,'PUBLISHED',NULL,'B1','Question 5 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:30.795882',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:30.795882',NULL),(36,NULL,NULL,'PUBLISHED',NULL,'B1','Question 6 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:30.804099',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:30.804099',NULL),(37,NULL,NULL,'PUBLISHED',NULL,'B1','Question 7 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:30.818211',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:30.818211',NULL),(38,NULL,NULL,'PUBLISHED',NULL,'B1','Question 8 for Quiz 1 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:30.836758',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:30.836758',NULL),(39,NULL,NULL,'PUBLISHED',NULL,'B1','Question 9 for Quiz 1 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:30.859128',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:30.859128',NULL),(40,NULL,NULL,'PUBLISHED',NULL,'B1','Question 10 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:30.874456',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:30.874456',NULL),(41,NULL,NULL,'PUBLISHED',NULL,'B1','Question 11 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:30.885778',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:30.885778',NULL),(42,NULL,NULL,'PUBLISHED',NULL,'B1','Question 12 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:30.904236',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:30.904236',NULL),(43,NULL,NULL,'PUBLISHED',NULL,'B1','Question 13 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:30.915619',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:30.915619',NULL),(44,NULL,NULL,'PUBLISHED',NULL,'B1','Question 14 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:30.934043',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:30.934043',NULL),(45,NULL,NULL,'PUBLISHED',NULL,'B1','Question 15 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:30.947403',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:30.947403',NULL),(46,NULL,NULL,'PUBLISHED',NULL,'B1','Question 16 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:30.958297',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:30.958297',NULL),(47,NULL,NULL,'PUBLISHED',NULL,'B1','Question 17 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:30.974650',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:30.974650',NULL),(48,NULL,NULL,'PUBLISHED',NULL,'B1','Question 18 for Quiz 1 (Type: MATCHING)','2026-03-24 15:46:30.990396',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:30.990396',NULL),(49,NULL,NULL,'PUBLISHED',NULL,'B1','Question 19 for Quiz 1 (Type: MATCHING)','2026-03-24 15:46:31.000926',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.000926',NULL),(50,NULL,NULL,'PUBLISHED',NULL,'B1','Question 20 for Quiz 1 (Type: MATCHING)','2026-03-24 15:46:31.011522',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.011522',NULL),(51,NULL,NULL,'PUBLISHED',NULL,'B1','Question 21 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.025506',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.025506',NULL),(52,NULL,NULL,'PUBLISHED',NULL,'B1','Question 22 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.038222',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.038222',NULL),(53,NULL,NULL,'PUBLISHED',NULL,'B1','Question 23 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.060542',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.060542',NULL),(54,NULL,NULL,'PUBLISHED',NULL,'B1','Question 24 for Quiz 1 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.079224',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.079224',NULL),(55,NULL,NULL,'PUBLISHED',NULL,'B1','Question 25 for Quiz 1 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.093170',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.093170',NULL),(56,NULL,NULL,'PUBLISHED',NULL,'B1','Question 26 for Quiz 1 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.123044',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.123044',NULL),(57,NULL,NULL,'PUBLISHED',NULL,'B1','Question 27 for Quiz 1 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.145163',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.145163',NULL),(58,NULL,NULL,'PUBLISHED',NULL,'B1','Question 28 for Quiz 1 (Type: MATCHING)','2026-03-24 15:46:31.159282',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.159282',NULL),(59,NULL,NULL,'PUBLISHED',NULL,'B1','Question 29 for Quiz 1 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.169355',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.169355',NULL),(60,NULL,NULL,'PUBLISHED',NULL,'B1','Question 30 for Quiz 1 (Type: MATCHING)','2026-03-24 15:46:31.180772',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.180772',NULL),(61,NULL,NULL,'PUBLISHED',NULL,'B1','Question 1 for Quiz 2 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.267687',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.267687',NULL),(62,NULL,NULL,'PUBLISHED',NULL,'B1','Question 2 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.290832',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.290832',NULL),(63,NULL,NULL,'PUBLISHED',NULL,'B1','Question 3 for Quiz 2 (Type: MATCHING)','2026-03-24 15:46:31.309535',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.309535',NULL),(64,NULL,NULL,'PUBLISHED',NULL,'B1','Question 4 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.327100',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.327100',NULL),(65,NULL,NULL,'PUBLISHED',NULL,'B1','Question 5 for Quiz 2 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.348031',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.348031',NULL),(66,NULL,NULL,'PUBLISHED',NULL,'B1','Question 6 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.358548',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.358548',NULL),(67,NULL,NULL,'PUBLISHED',NULL,'B1','Question 7 for Quiz 2 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.371085',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.371085',NULL),(68,NULL,NULL,'PUBLISHED',NULL,'B1','Question 8 for Quiz 2 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.389056',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.389056',NULL),(69,NULL,NULL,'PUBLISHED',NULL,'B1','Question 9 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.404239',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.404239',NULL),(70,NULL,NULL,'PUBLISHED',NULL,'B1','Question 10 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.415297',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.415297',NULL),(71,NULL,NULL,'PUBLISHED',NULL,'B1','Question 11 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.425394',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.425394',NULL),(72,NULL,NULL,'PUBLISHED',NULL,'B1','Question 12 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.435915',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.435915',NULL),(73,NULL,NULL,'PUBLISHED',NULL,'B1','Question 13 for Quiz 2 (Type: MATCHING)','2026-03-24 15:46:31.454997',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.454997',NULL),(74,NULL,NULL,'PUBLISHED',NULL,'B1','Question 14 for Quiz 2 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.473433',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.473433',NULL),(75,NULL,NULL,'PUBLISHED',NULL,'B1','Question 15 for Quiz 2 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.499189',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.499189',NULL),(76,NULL,NULL,'PUBLISHED',NULL,'B1','Question 16 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.512386',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.512386',NULL),(77,NULL,NULL,'PUBLISHED',NULL,'B1','Question 17 for Quiz 2 (Type: MATCHING)','2026-03-24 15:46:31.536216',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.536216',NULL),(78,NULL,NULL,'PUBLISHED',NULL,'B1','Question 18 for Quiz 2 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.553489',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.553489',NULL),(79,NULL,NULL,'PUBLISHED',NULL,'B1','Question 19 for Quiz 2 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.570435',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.570435',NULL),(80,NULL,NULL,'PUBLISHED',NULL,'B1','Question 20 for Quiz 2 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.582351',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.582351',NULL),(81,NULL,NULL,'PUBLISHED',NULL,'B1','Question 21 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.597696',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.597696',NULL),(82,NULL,NULL,'PUBLISHED',NULL,'B1','Question 22 for Quiz 2 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.610599',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.610599',NULL),(83,NULL,NULL,'PUBLISHED',NULL,'B1','Question 23 for Quiz 2 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.623452',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.623452',NULL),(84,NULL,NULL,'PUBLISHED',NULL,'B1','Question 24 for Quiz 2 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.631441',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.631441',NULL),(85,NULL,NULL,'PUBLISHED',NULL,'B1','Question 25 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.646038',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.646038',NULL),(86,NULL,NULL,'PUBLISHED',NULL,'B1','Question 26 for Quiz 2 (Type: MATCHING)','2026-03-24 15:46:31.659644',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.659644',NULL),(87,NULL,NULL,'PUBLISHED',NULL,'B1','Question 27 for Quiz 2 (Type: MATCHING)','2026-03-24 15:46:31.674006',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.674006',NULL),(88,NULL,NULL,'PUBLISHED',NULL,'B1','Question 28 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.689874',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.689874',NULL),(89,NULL,NULL,'PUBLISHED',NULL,'B1','Question 29 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.702033',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.702033',NULL),(90,NULL,NULL,'PUBLISHED',NULL,'B1','Question 30 for Quiz 2 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.715884',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.715884',NULL),(91,NULL,NULL,'PUBLISHED',NULL,'B1','Question 1 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.799401',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.799401',NULL),(92,NULL,NULL,'PUBLISHED',NULL,'B1','Question 2 for Quiz 3 (Type: MATCHING)','2026-03-24 15:46:31.813862',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.813862',NULL),(93,NULL,NULL,'PUBLISHED',NULL,'B1','Question 3 for Quiz 3 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.826398',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.826398',NULL),(94,NULL,NULL,'PUBLISHED',NULL,'B1','Question 4 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.836258',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.836258',NULL),(95,NULL,NULL,'PUBLISHED',NULL,'B1','Question 5 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.848533',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.848533',NULL),(96,NULL,NULL,'PUBLISHED',NULL,'B1','Question 6 for Quiz 3 (Type: MATCHING)','2026-03-24 15:46:31.860203',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.860203',NULL),(97,NULL,NULL,'PUBLISHED',NULL,'B1','Question 7 for Quiz 3 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.876026',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.876026',NULL),(98,NULL,NULL,'PUBLISHED',NULL,'B1','Question 8 for Quiz 3 (Type: MATCHING)','2026-03-24 15:46:31.892854',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:31.892854',NULL),(99,NULL,NULL,'PUBLISHED',NULL,'B1','Question 9 for Quiz 3 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.905367',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.905367',NULL),(100,NULL,NULL,'PUBLISHED',NULL,'B1','Question 10 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.924680',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.924680',NULL),(101,NULL,NULL,'PUBLISHED',NULL,'B1','Question 11 for Quiz 3 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.937091',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.937091',NULL),(102,NULL,NULL,'PUBLISHED',NULL,'B1','Question 12 for Quiz 3 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.954545',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.954545',NULL),(103,NULL,NULL,'PUBLISHED',NULL,'B1','Question 13 for Quiz 3 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:31.965212',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:31.965212',NULL),(104,NULL,NULL,'PUBLISHED',NULL,'B1','Question 14 for Quiz 3 (Type: FILL_IN_BLANK)','2026-03-24 15:46:31.978560',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:31.978560',NULL),(105,NULL,NULL,'PUBLISHED',NULL,'B1','Question 15 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.987959',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.987959',NULL),(106,NULL,NULL,'PUBLISHED',NULL,'B1','Question 16 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:31.998351',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:31.998351',NULL),(107,NULL,NULL,'PUBLISHED',NULL,'B1','Question 17 for Quiz 3 (Type: MATCHING)','2026-03-24 15:46:32.007976',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:32.007976',NULL),(108,NULL,NULL,'PUBLISHED',NULL,'B1','Question 18 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:32.019575',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:32.019575',NULL),(109,NULL,NULL,'PUBLISHED',NULL,'B1','Question 19 for Quiz 3 (Type: MATCHING)','2026-03-24 15:46:32.029057',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:32.029057',NULL),(110,NULL,NULL,'PUBLISHED',NULL,'B1','Question 20 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:32.038133',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:32.038133',NULL),(111,NULL,NULL,'PUBLISHED',NULL,'B1','Question 21 for Quiz 3 (Type: MATCHING)','2026-03-24 15:46:32.049190',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:32.049190',NULL),(112,NULL,NULL,'PUBLISHED',NULL,'B1','Question 22 for Quiz 3 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:32.078665',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:32.078665',NULL),(113,NULL,NULL,'PUBLISHED',NULL,'B1','Question 23 for Quiz 3 (Type: FILL_IN_BLANK)','2026-03-24 15:46:32.126186',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:32.126186',NULL),(114,NULL,NULL,'PUBLISHED',NULL,'B1','Question 24 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:32.137698',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:32.137698',NULL),(115,NULL,NULL,'PUBLISHED',NULL,'B1','Question 25 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:32.151463',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:32.151463',NULL),(116,NULL,NULL,'PUBLISHED',NULL,'B1','Question 26 for Quiz 3 (Type: FILL_IN_BLANK)','2026-03-24 15:46:32.168906',NULL,NULL,'FILL_IN_BLANK','General',NULL,NULL,'2026-03-24 15:46:32.168906',NULL),(117,NULL,NULL,'PUBLISHED',NULL,'B1','Question 27 for Quiz 3 (Type: MULTIPLE_CHOICE_SINGLE)','2026-03-24 15:46:32.180574',NULL,NULL,'MULTIPLE_CHOICE_SINGLE','General',NULL,NULL,'2026-03-24 15:46:32.180574',NULL),(118,NULL,NULL,'PUBLISHED',NULL,'B1','Question 28 for Quiz 3 (Type: MATCHING)','2026-03-24 15:46:32.194176',NULL,NULL,'MATCHING','General',NULL,NULL,'2026-03-24 15:46:32.194176',NULL),(119,NULL,NULL,'PUBLISHED',NULL,'B1','Question 29 for Quiz 3 (Type: MATCHING)','2026-03-24 15:46:32.207041',NULL,NULL,'MULTIPLE_CHOICE_MULTI','READING','a 1','Thú cưng','2026-03-26 02:22:47.461178',NULL),(120,NULL,NULL,'PUBLISHED',NULL,'B1','Question 30 for Quiz 3 (Type: MULTIPLE_CHOICE_MULTI)','2026-03-24 15:46:32.218195',NULL,NULL,'MULTIPLE_CHOICE_MULTI','General',NULL,NULL,'2026-03-24 15:46:32.218195',NULL);
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
  `course_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `class_id` int DEFAULT NULL,
  `quiz_category` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `max_attempts` int DEFAULT NULL,
  `number_of_questions` int DEFAULT NULL,
  `pass_score` decimal(5,2) DEFAULT NULL,
  `question_order` varchar(10) DEFAULT NULL,
  `show_answer_after_submit` bit(1) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `time_limit_minutes` int DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `is_open` bit(1) DEFAULT NULL,
  PRIMARY KEY (`quiz_id`),
  KEY `course_id` (`course_id`),
  KEY `user_id` (`user_id`),
  KEY `class_id` (`class_id`),
  CONSTRAINT `quiz_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `quiz_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `quiz_ibfk_3` FOREIGN KEY (`class_id`) REFERENCES `class` (`class_id`)
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz`
--

LOCK TABLES `quiz` WRITE;
/*!40000 ALTER TABLE `quiz` DISABLE KEYS */;
INSERT INTO `quiz` VALUES (201,201,203,201,'ENTRY_TEST',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(202,206,220,212,'COURSE_QUIZ','2026-03-24 15:46:29.359748','Bài test kiểm tra kỹ năng Skimming & Scanning (Match headings, T/F/NG)',3,30,50.00,'RANDOM',_binary '','PUBLISHED',60,'IELTS Reading Mini-Test 1','2026-03-24 15:46:29.359748',NULL),(203,207,223,NULL,'COURSE_QUIZ','2026-03-24 15:46:30.736528','Complete this quiz to test your module knowledge.',NULL,30,50.00,'RANDOM',_binary '','PUBLISHED',60,'Final Quiz for Course Title 1','2026-03-24 15:46:30.736528',NULL),(204,208,224,NULL,'COURSE_QUIZ','2026-03-24 15:46:31.263185','Complete this quiz to test your module knowledge.',NULL,30,50.00,'RANDOM',_binary '','PUBLISHED',60,'Final Quiz for Course Title 2','2026-03-24 15:46:31.263185',NULL),(205,209,223,NULL,'COURSE_QUIZ','2026-03-24 15:46:31.796221','Complete this quiz to test your module knowledge.',NULL,30,50.00,'RANDOM',_binary '','PUBLISHED',60,'Final Quiz for Course Title 3','2026-03-24 15:46:31.796221',NULL),(206,201,203,NULL,'COURSE_QUIZ','2026-03-24 16:22:28.164147','1234',2,NULL,70.00,'RANDOM',_binary '','PUBLISHED',30,'test2','2026-03-24 16:23:17.559033',NULL),(207,102,203,103,'COURSE_QUIZ','2026-03-24 16:51:19.038400','đẹp trai có gì sai',2,NULL,50.00,'RANDOM',_binary '','PUBLISHED',20,'Đạt đẹp trai','2026-03-24 16:51:44.204618',NULL),(208,102,203,NULL,'COURSE_QUIZ','2026-03-24 17:06:38.626171','đă',-1,NULL,50.00,'RANDOM',_binary '','PUBLISHED',20,'ewqewq','2026-03-24 17:09:19.247414',NULL),(209,NULL,203,NULL,'ENTRY_TEST','2026-03-24 17:10:35.666637','ewqeqw',2,NULL,50.00,'RANDOM',_binary '','PUBLISHED',15,'eqweqe','2026-03-24 17:11:22.450094',NULL),(210,NULL,203,NULL,'ENTRY_TEST','2026-03-24 17:12:28.864969','caww',NULL,NULL,40.00,'RANDOM',_binary '','PUBLISHED',12,'cac','2026-03-24 17:12:53.924061',NULL),(211,102,203,NULL,'COURSE_QUIZ','2026-03-25 09:38:57.781468','abcd',2,NULL,20.00,'FIXED',_binary '','PUBLISHED',5,'1234','2026-03-25 09:38:57.781468',NULL),(212,210,203,NULL,'COURSE_QUIZ','2026-03-25 15:09:23.247188','chơi đi',2,NULL,30.00,'RANDOM',_binary '','PUBLISHED',20,'Học làm gì','2026-03-25 15:09:23.247188',NULL),(213,210,203,NULL,'COURSE_QUIZ','2026-03-25 15:12:51.463984','nnana',2,NULL,56.00,'RANDOM',_binary '','PUBLISHED',12,'Ngủ thật sâu','2026-03-25 15:12:51.463984',NULL),(214,NULL,203,NULL,'ENTRY_TEST','2026-03-25 15:43:23.223923','123',1,NULL,40.00,'FIXED',_binary '','PUBLISHED',20,'test1','2026-03-25 15:43:33.092505',NULL);
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
  `is_correct` bit(1) DEFAULT NULL,
  PRIMARY KEY (`answer_id`),
  KEY `result_id` (`result_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `quiz_answer_ibfk_1` FOREIGN KEY (`result_id`) REFERENCES `quiz_result` (`result_id`),
  CONSTRAINT `quiz_answer_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz_answer`
--

LOCK TABLES `quiz_answer` WRITE;
/*!40000 ALTER TABLE `quiz_answer` DISABLE KEYS */;
INSERT INTO `quiz_answer` VALUES (1,203,119,'{\"238\":\"\",\"239\":\"\"}',_binary '\0'),(2,203,118,'{\"236\":\"\",\"237\":\"\"}',_binary '\0'),(3,203,117,'234',_binary ''),(4,203,116,'\"dsa\"',_binary '\0'),(5,203,115,'231',_binary ''),(6,203,114,'230',_binary '\0'),(7,203,113,'\"adsadsadasdasdasddsadas\"',_binary '\0'),(8,203,112,'225',_binary '\0'),(9,204,119,'{\"238\":\"2\",\"239\":\"2\"}',_binary '\0'),(10,204,118,'{\"236\":\"2\",\"237\":\"1\"}',_binary '\0'),(11,204,117,'234',_binary ''),(12,204,116,'\"ưqe\"',_binary '\0'),(13,204,115,'232',_binary '\0'),(14,204,114,'229',_binary ''),(15,204,113,'\"ưqew\"',_binary '\0'),(16,204,112,'226',_binary '\0'),(17,205,22,'{\"46\":\"Description 2\",\"47\":\"Description 1\"}',_binary '\0'),(18,205,21,'\"dsa\"',_binary '\0'),(19,205,24,'\"đá\"',_binary '\0'),(20,205,25,'\"fas\"',_binary ''),(21,205,27,'54',_binary '\0');
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
  `order_index` int DEFAULT NULL,
  `points` decimal(5,2) DEFAULT NULL,
  PRIMARY KEY (`quiz_question_id`),
  KEY `quiz_id` (`quiz_id`),
  KEY `question_id` (`question_id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `quiz_question_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`),
  CONSTRAINT `quiz_question_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`),
  CONSTRAINT `quiz_question_ibfk_3` FOREIGN KEY (`group_id`) REFERENCES `question_group` (`group_id`)
) ENGINE=InnoDB AUTO_INCREMENT=174 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz_question`
--

LOCK TABLES `quiz_question` WRITE;
/*!40000 ALTER TABLE `quiz_question` DISABLE KEYS */;
INSERT INTO `quiz_question` VALUES (1,202,1,NULL,1,10.00),(2,202,2,NULL,2,10.00),(3,202,3,NULL,3,10.00),(4,202,4,NULL,4,10.00),(5,202,5,NULL,5,10.00),(6,202,6,NULL,6,10.00),(7,202,7,NULL,7,10.00),(8,202,8,NULL,8,10.00),(9,202,9,NULL,9,10.00),(10,202,10,NULL,10,10.00),(11,202,11,NULL,11,10.00),(12,202,12,NULL,12,10.00),(13,202,13,NULL,13,10.00),(14,202,14,NULL,14,10.00),(15,202,15,NULL,15,10.00),(16,202,16,NULL,16,10.00),(17,202,17,NULL,17,10.00),(18,202,18,NULL,18,10.00),(19,202,19,NULL,19,10.00),(20,202,20,NULL,20,10.00),(21,202,21,NULL,21,10.00),(22,202,22,NULL,22,10.00),(23,202,23,NULL,23,10.00),(24,202,24,NULL,24,10.00),(25,202,25,NULL,25,10.00),(26,202,26,NULL,26,10.00),(27,202,27,NULL,27,10.00),(28,202,28,NULL,28,10.00),(29,202,29,NULL,29,10.00),(30,202,30,NULL,30,10.00),(31,203,31,NULL,1,1.00),(32,203,32,NULL,2,1.00),(33,203,33,NULL,3,1.00),(34,203,34,NULL,4,1.00),(35,203,35,NULL,5,1.00),(36,203,36,NULL,6,1.00),(37,203,37,NULL,7,1.00),(38,203,38,NULL,8,1.00),(39,203,39,NULL,9,1.00),(40,203,40,NULL,10,1.00),(41,203,41,NULL,11,1.00),(42,203,42,NULL,12,1.00),(43,203,43,NULL,13,1.00),(44,203,44,NULL,14,1.00),(45,203,45,NULL,15,1.00),(46,203,46,NULL,16,1.00),(47,203,47,NULL,17,1.00),(48,203,48,NULL,18,1.00),(49,203,49,NULL,19,1.00),(50,203,50,NULL,20,1.00),(51,203,51,NULL,21,1.00),(52,203,52,NULL,22,1.00),(53,203,53,NULL,23,1.00),(54,203,54,NULL,24,1.00),(55,203,55,NULL,25,1.00),(56,203,56,NULL,26,1.00),(57,203,57,NULL,27,1.00),(58,203,58,NULL,28,1.00),(59,203,59,NULL,29,1.00),(60,203,60,NULL,30,1.00),(61,204,61,NULL,1,1.00),(62,204,62,NULL,2,1.00),(63,204,63,NULL,3,1.00),(64,204,64,NULL,4,1.00),(65,204,65,NULL,5,1.00),(66,204,66,NULL,6,1.00),(67,204,67,NULL,7,1.00),(68,204,68,NULL,8,1.00),(69,204,69,NULL,9,1.00),(70,204,70,NULL,10,1.00),(71,204,71,NULL,11,1.00),(72,204,72,NULL,12,1.00),(73,204,73,NULL,13,1.00),(74,204,74,NULL,14,1.00),(75,204,75,NULL,15,1.00),(76,204,76,NULL,16,1.00),(77,204,77,NULL,17,1.00),(78,204,78,NULL,18,1.00),(79,204,79,NULL,19,1.00),(80,204,80,NULL,20,1.00),(81,204,81,NULL,21,1.00),(82,204,82,NULL,22,1.00),(83,204,83,NULL,23,1.00),(84,204,84,NULL,24,1.00),(85,204,85,NULL,25,1.00),(86,204,86,NULL,26,1.00),(87,204,87,NULL,27,1.00),(88,204,88,NULL,28,1.00),(89,204,89,NULL,29,1.00),(90,204,90,NULL,30,1.00),(91,205,91,NULL,1,1.00),(92,205,92,NULL,2,1.00),(93,205,93,NULL,3,1.00),(94,205,94,NULL,4,1.00),(95,205,95,NULL,5,1.00),(96,205,96,NULL,6,1.00),(97,205,97,NULL,7,1.00),(98,205,98,NULL,8,1.00),(99,205,99,NULL,9,1.00),(100,205,100,NULL,10,1.00),(101,205,101,NULL,11,1.00),(102,205,102,NULL,12,1.00),(103,205,103,NULL,13,1.00),(104,205,104,NULL,14,1.00),(105,205,105,NULL,15,1.00),(106,205,106,NULL,16,1.00),(107,205,107,NULL,17,1.00),(108,205,108,NULL,18,1.00),(109,205,109,NULL,19,1.00),(110,205,110,NULL,20,1.00),(111,205,111,NULL,21,1.00),(112,205,112,NULL,22,1.00),(113,205,113,NULL,23,1.00),(114,205,114,NULL,24,1.00),(115,205,115,NULL,25,1.00),(116,205,116,NULL,26,1.00),(117,205,117,NULL,27,1.00),(118,205,118,NULL,28,1.00),(119,205,119,NULL,29,1.00),(120,205,120,NULL,30,1.00),(121,206,120,NULL,1,1.00),(122,206,119,NULL,2,1.00),(123,206,118,NULL,3,1.00),(124,206,117,NULL,4,1.00),(125,206,116,NULL,5,1.00),(126,207,119,NULL,1,1.00),(127,207,118,NULL,2,1.00),(128,207,117,NULL,3,1.00),(129,207,116,NULL,4,1.00),(130,207,115,NULL,5,1.00),(131,207,114,NULL,6,1.00),(132,207,113,NULL,7,1.00),(133,207,112,NULL,8,1.00),(134,208,28,NULL,4,1.00),(135,208,18,NULL,1,1.00),(136,208,10,NULL,6,1.00),(137,208,8,NULL,3,1.00),(138,209,111,NULL,1,1.00),(139,209,112,NULL,2,1.00),(140,209,114,NULL,3,1.00),(141,209,115,NULL,4,1.00),(142,209,117,NULL,5,1.00),(143,209,119,NULL,6,1.00),(144,210,78,NULL,1,1.00),(145,210,117,NULL,2,1.00),(146,210,118,NULL,3,1.00),(147,210,119,NULL,4,1.00),(148,210,120,NULL,5,1.00),(149,210,115,NULL,6,1.00),(150,210,114,NULL,7,1.00),(151,210,112,NULL,8,1.00),(152,210,111,NULL,9,1.00),(153,211,119,NULL,1,1.00),(154,211,118,NULL,2,1.00),(155,211,117,NULL,3,1.00),(156,211,116,NULL,4,1.00),(157,211,115,NULL,5,1.00),(158,212,22,NULL,1,1.00),(159,212,21,NULL,2,1.00),(160,212,24,NULL,3,1.00),(161,212,25,NULL,4,1.00),(162,212,27,NULL,5,1.00),(163,213,120,NULL,1,1.00),(164,213,119,NULL,2,1.00),(165,213,118,NULL,3,1.00),(166,213,117,NULL,4,1.00),(167,213,116,NULL,5,1.00),(168,214,120,NULL,1,1.00),(169,214,119,NULL,2,1.00),(170,214,118,NULL,3,1.00),(171,214,117,NULL,4,1.00),(172,214,115,NULL,5,1.00),(173,214,114,NULL,6,1.00);
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
  `passed` bit(1) DEFAULT NULL,
  `score` int DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`result_id`),
  KEY `quiz_id` (`quiz_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `quiz_result_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`),
  CONSTRAINT `quiz_result_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quiz_result`
--

LOCK TABLES `quiz_result` WRITE;
/*!40000 ALTER TABLE `quiz_result` DISABLE KEYS */;
INSERT INTO `quiz_result` VALUES (201,201,206,0.00,_binary '',0,NULL),(202,201,207,0.00,_binary '',0,NULL),(203,207,2,25.00,_binary '\0',2,'2026-03-24 17:57:29.848886'),(204,207,2,25.00,_binary '\0',2,'2026-03-25 09:18:29.911311'),(205,212,2,20.00,_binary '\0',1,'2026-03-25 15:13:51.906889');
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
) ENGINE=InnoDB AUTO_INCREMENT=218 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registration`
--

LOCK TABLES `registration` WRITE;
/*!40000 ALTER TABLE `registration` DISABLE KEYS */;
INSERT INTO `registration` VALUES (101,2,101,101,'2026-02-20 10:00:00',5000000.00,'Cancelled','Đăng ký chờ duyệt (Test Hủy)'),(102,2,103,102,'2026-01-15 08:30:00',3500000.00,'Approved','Đã thanh toán (Test My Courses)'),(103,2,102,101,'2025-12-01 14:00:00',5000000.00,'Cancelled','Hủy do bận lịch (Test hiển thị)'),(201,206,201,201,'2026-03-02 10:49:44',5500000.00,'PAID',NULL),(202,207,201,201,'2026-03-02 10:49:44',5500000.00,'PAID',NULL),(203,208,202,201,'2026-03-02 10:49:44',5200000.00,'PAID',NULL),(204,206,203,202,'2026-03-02 10:49:44',3000000.00,'PAID',NULL),(205,219,212,206,'2026-03-24 15:46:29',1500000.00,'Approved',NULL),(206,2,213,201,'2026-03-24 16:30:52',0.00,'Approved',''),(207,2,201,205,'2026-03-25 08:57:24',5000000.00,'Rejected',''),(208,2,209,203,'2026-03-25 08:57:46',0.00,'Approved','Đăng ký trực tuyến, chờ thanh toán PayOS'),(209,2,217,210,'2026-03-25 15:01:35',10000.00,'Approved',''),(210,206,220,210,'2026-03-25 15:31:46',10000.00,'Approved',''),(211,3,209,203,'2026-03-25 16:14:52',0.00,'Submitted','Đăng ký trực tuyến, chờ thanh toán PayOS'),(212,3,217,210,'2026-03-25 16:15:23',10000.00,'Cancelled','Đăng ký trực tuyến, chờ thanh toán PayOS'),(213,3,201,205,'2026-03-25 16:15:51',5000000.00,'PENDING','Đăng ký trực tuyến, chờ thanh toán PayOS'),(214,3,217,210,'2026-03-26 02:24:17',10000.00,'PENDING','Đăng ký trực tuyến, chờ thanh toán PayOS'),(215,206,213,201,'2026-03-27 04:49:00',0.00,'Approved','Đăng ký trực tuyến, chờ thanh toán PayOS'),(216,3,220,210,'2026-03-27 09:37:52',10000.00,'PENDING','Đăng ký trực tuyến, chờ thanh toán PayOS'),(217,3,220,210,'2026-03-27 09:37:53',10000.00,'PENDING','Đăng ký trực tuyến, chờ thanh toán PayOS');
/*!40000 ALTER TABLE `registration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `session_quiz`
--

DROP TABLE IF EXISTS `session_quiz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_quiz` (
  `id` int NOT NULL AUTO_INCREMENT,
  `is_open` bit(1) DEFAULT NULL,
  `order_index` int DEFAULT NULL,
  `quiz_id` int NOT NULL,
  `session_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqblyxvmbdxk7e0gg0n860koyl` (`quiz_id`),
  KEY `FK20pwcrd4c20j6i7t991d3geeq` (`session_id`),
  CONSTRAINT `FK20pwcrd4c20j6i7t991d3geeq` FOREIGN KEY (`session_id`) REFERENCES `class_session` (`session_id`),
  CONSTRAINT `FKqblyxvmbdxk7e0gg0n860koyl` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`quiz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `session_quiz`
--

LOCK TABLES `session_quiz` WRITE;
/*!40000 ALTER TABLE `session_quiz` DISABLE KEYS */;
/*!40000 ALTER TABLE `session_quiz` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `setting`
--

LOCK TABLES `setting` WRITE;
/*!40000 ALTER TABLE `setting` DISABLE KEYS */;
INSERT INTO `setting` VALUES (101,'IELTS Preparation','CAT_IELTS','COURSE_CATEGORY',NULL,'Active',NULL),(102,'TOEIC Preparation','CAT_TOEIC','COURSE_CATEGORY',NULL,'Active',NULL),(201,'ROLE_ADMIN',NULL,'ROLE',NULL,'ACTIVE','Quản trị viên'),(202,'ROLE_MANAGER',NULL,'ROLE',NULL,'ACTIVE','Quản lý đào tạo'),(203,'ROLE_EXPERT',NULL,'ROLE',NULL,'ACTIVE','Chuyên gia nội dung'),(204,'ROLE_TEACHER',NULL,'ROLE',NULL,'ACTIVE','Giảng viên'),(205,'ROLE_STUDENT',NULL,'ROLE',NULL,'ACTIVE','Học viên'),(206,'IELTS Academic',NULL,'COURSE_CATEGORY',NULL,'ACTIVE','Luyện thi IELTS'),(207,'TOEIC 2 Kỹ năng',NULL,'COURSE_CATEGORY',NULL,'ACTIVE','Luyện thi TOEIC'),(208,'English Communication',NULL,'COURSE_CATEGORY',NULL,'ACTIVE','Giao tiếp phản xạ'),(209,'Admin','ROLE_ADMIN','USER_ROLE',1,'Active','System Administrator'),(210,'Student','ROLE_STUDENT','USER_ROLE',2,'Active','Learner'),(211,'Teacher','ROLE_TEACHER','USER_ROLE',3,'Active','Instructor'),(212,'Manager','ROLE_MANAGER','USER_ROLE',4,'Active','Course & Staff Manager'),(213,'IELTS Preparation','IELTS','COURSE_CATEGORY',NULL,'Active',NULL),(214,'General English','GEN','COURSE_CATEGORY',NULL,'Active',NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=227 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'System Administrator','admin@novalms.edu.vn','0769169665','$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',201,'https://res.cloudinary.com/decqv3ed0/image/upload/v1774622623/mf1ezcjcq731y2kgotgl.png',NULL,'Active','fgfffdfdf','LOCAL',NULL,NULL,NULL),(2,'Nart C','thanhmche172833@fpt.edu.vn','098761234','$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,'https://lh3.googleusercontent.com/a/ACg8ocKSEv_g3GPLFxJGd8kEfncwPnHNjletUQZcbNlhmCntdBwW4x7k=s96-c','2026-03-04 14:50:14','Active','sdsd123 2','GOOGLE',NULL,NULL,NULL),(3,'NGUYEN LONG THANH','maichithanh317@gmail.com','0989878056','$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,NULL,NULL,'Active',NULL,'LOCAL','Hanoi','Male',NULL),(4,'C nẹt','maingaquynhanh@gmail.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,'https://lh3.googleusercontent.com/a/ACg8ocLDzs7bIK3rqgxTOnHWfq40sL3LiRIHs6_AdH0E4vrFfzHL-1n6=s96-c','2026-02-10 08:01:19','Active',NULL,'GOOGLE',NULL,NULL,NULL),(101,'Manager Admin','manager@novalms.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',202,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(102,'Expert AI','expert@novalms.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',NULL,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(103,'Teacher John','teacher@novalms.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',NULL,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(104,'Course Manager','manager@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(201,'System Admin','admin@center.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',201,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(202,'Manager Nguyen','manager@center.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',202,NULL,NULL,'Active',NULL,NULL,NULL,NULL,NULL),(203,'Expert Tran','expert@center.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',203,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(204,'Teacher Hoang','teacher1@center.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',204,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(205,'Teacher Le','teacher2@center.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',204,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(206,'Student An','student1@gmail.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(207,'Student Binh','student2@gmail.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(208,'Student Cuong','student3@gmail.com',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,NULL,NULL,'ACTIVE',NULL,NULL,NULL,NULL,NULL),(211,'Nguyễn Chu Tứ','chutu1212141@gmail.com','0968515656','$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(212,'Nguyen Chu Tu K17 HL','tunche170493@fpt.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,'https://lh3.googleusercontent.com/a/ACg8ocI4LEjoC6mlg2U8Sx2ap3u87o-h4FViSEAkV472XdjSG8XM=s96-c','2026-03-04 13:31:51','Active',NULL,'GOOGLE',NULL,NULL,NULL),(213,'Course Manager','student@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(216,'Nguyễn Chu Tứ','chutu@gmail.com','0968716203','$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',201,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(217,'Nguyễn Chu Tứ','chu@gmail.com','0968771659','$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(218,'Nguyễn Chu Tứ','chu56@gmail.com','6659896238','$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',202,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(219,'Student Test','student_test@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(220,'Teacher Test','teacher_test@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',204,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(221,'Expert Name 1','expert1@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',203,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(222,'Expert Name 2','expert2@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',203,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(223,'Teacher Name 1','teacher1@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',204,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(224,'Teacher Name 2','teacher2@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',204,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(225,'Student Name 1','student1@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL),(226,'Student Name 2','student2@novalms.edu.vn',NULL,'$2a$12$4VT.nAlBTqwA4CJLiheLKuft87eyMvCJQuArJqXz31Z0Ux.i8z7Ca',205,NULL,NULL,'Active',NULL,'LOCAL',NULL,NULL,NULL);
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
INSERT INTO `user_lesson` VALUES (2,256,'Completed'),(2,257,'Completed'),(206,259,'Completed');
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

-- Dump completed on 2026-03-28 19:46:59
