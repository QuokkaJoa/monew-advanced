
# 📰 Monew - News Management System

> 뉴스 수집, 관리, 백업을 위한 Spring Boot 기반 웹 애플리케이션

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AWS](https://img.shields.io/badge/AWS-ECS%20%7C%20S3-yellow.svg)](https://aws.amazon.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)

## 🚀 주요 기능

- **뉴스 수집**: 다양한 뉴스 소스에서 자동 수집
- **데이터 관리**: 뉴스 기사 CRUD 및 검색 기능
- **자동 백업**: 일일 뉴스 데이터 , 로그 S3 백업
- **사용자 활동**: 조회수, 댓글 관리
- **배치 처리**: 스케줄링 기반 자동화 작업

## 🏗️ 시스템 아키텍처
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Spring Boot   │    │   PostgreSQL    │
│   (Web/Mobile)  │◄──►│   Application   │◄──►│   Database      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │     AWS S3      │
                       │  (File Storage) │
                       └─────────────────┘

## 🛠️ 기술 스택

### Backend
- **Java 17**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **Spring Security**
- **Spring Batch**

### Database
- **PostgreSQL 15**

### Infrastructure
- **AWS ECS** (컨테이너 배포)
- **AWS S3** (파일 저장소)
- **Docker** (컨테이너화)

### CI/CD
- **GitHub Actions**
- **AWS ECR** (컨테이너 레지스트리)


## 📡 API 문서

### 주요 엔드포인트

#### 백업 관리
- `POST /api/backup/restore` - 백업 복원
### 사용자 관리
- `POST /api/users` - 회원가입
- `POST /api/users/login` - 로그인
- `PATCH /api/users/{userId}` - 프로필 수정
- `DELETE /api/users/{userId}` - 회원탈퇴

### 뉴스 기사
- `GET /api/articles` - 기사 목록 조회
- `POST /api/articles/{articleId}/article-views` - 조회수 증가
- `GET /api/articles/sources` - 뉴스 소스 목록
- `GET /api/articles/restore` - 데이터 복원

### 댓글 시스템
- `GET /api/comments` - 댓글 목록
- `POST /api/comments` - 댓글 작성
- `POST /api/comments/{commentId}/comment-likes` - 댓글 좋아요

### 관심사 관리
- `POST /api/interests` - 관심사 등록
- `GET /api/interests` - 관심사 검색
- `POST /api/interests/{interestId}/subscriptions` - 구독

### 알림
- `GET /api/notifications` - 알림 목록
- `PATCH /api/notifications` - 알림 읽음 처리

### 사용자 통계
- `GET /api/user-activities/{userId}` - 활동 통계


### 2. 스케줄링
- **뉴스 수집**: 매시간 (0 0 0/1 * * *)
- **일일 백업**: 매일 자정 (0 0 0 * * *)
- **로그 업로드**: 매일 새벽 5분 (0 5 0 * * *)

## 🚀 배포

### GitHub Actions를 통한 자동 배포
1. `main` 브랜치에 코드 푸시
2. 자동으로 테스트 실행
3. Docker 이미지 빌드
4. AWS ECR에 이미지 푸시
5. AWS ECS에 자동 배포


## 팀원
- 김세은
- 김지협
- 심민혁
- 양진호
- 양찬혁

## 🔧 트러블슈팅
## 🛠️ 트러블 슈팅

| 내용 | 내용 |
|------|------|
| **발생한 문제** | AWS S3 키를 yaml에 넣은 상태에서 푸사가 불가능한 문제 발생 |
| **원인 분석** | AWS S3 키를 직접 넣고 푸시는 불가함 |
| **배운 점 및 개선 사항** | AWS S3 키를 .env파일로 관리 해야함 |
| **해결방안** | 커밋 히스토리에 있는 AWS S3키를 제거함 |

| 내용 | 내용 |
|------|------|
| **발생한 문제** | 로컬에서 인덱스를 걸고 성능테스트 하였을 때 성능이 향상되었지만 AWS에서는 그렇지 않은 문제 발생 |
| **원인 분석** |서버에 구성한 하드웨어가 성능 테스트를 할 수 있는 사양이 아니었음 |
| **배운 점 및 개선 사항** | 초기 단계에 성능 테스트 목표를 설정하여 성능 테스트를 해야함 |
| **해결방안** | 서버 구성을 다시하고, 초기 목표 값을 설정하여 성능 테스트를 해야함 |
