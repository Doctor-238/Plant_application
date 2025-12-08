## 🌱 AI 기반 스마트 식물 관리 애플리케이션 ##

> AI 분석 + 날씨 정보 + 사용자 맞춤 관리 기능을 결합한 스마트 식물 관리 앱

<img width="1920" height="1080" alt="planty" src="https://github.com/user-attachments/assets/ceb71881-4d13-450b-90ee-a91e67e5930e" />


## 📌 프로젝트 개요

**AI 기반 스마트 식물 관리 애플리케이션**은 사용자가 자신의 식물을 쉽고 체계적으로 관리할 수 있도록 돕는 모바일 앱입니다.

- 물 주기 / 살충제 사용 / 온도 등 주기적 관리 알람 제공
- 날씨 데이터 + AI 추천 기능을 결합해 사용자 환경에 맞는 식물을 제안
- 사진 분석 기반 AI 식물 검색
- 식물 관리 다이어리, Todo 리스트, 맞춤 알림 등 다양한 편의 기능 제공

사용자가 식물 관리 경험을 더 즐겁고 편리하게 할 수 있도록 설계되었습니다.

## 📱 실행 데모

<img width="1920" height="1080" alt="run" src="https://github.com/user-attachments/assets/52eafcc9-0958-4128-b4d2-5a6671a2a2e3" />


## 🛠 주요 기능

### 🌿 1. 내 식물 관리
- 식물 이미지, 정보, 상태 확인: 다이어리 화면에서 사용자가 등록한 모든 식물을 한눈에 확인할 수 있습니다. 특정 식물을 선택하면 식물 상세 화면으로 이동하여 이미지, 상태 등 상세 정보를 확인할 수 있습니다.
- 물 주기 / 살충제 사용 기록: 홈 화면의 물 주기 / 살충제 버튼을 통해 해당 식물에 물 또는 살충제를 사용한 기록을 쉽게 남길 수 있습니다.

### 🔔 2. 알람 기능
- 물 주기, 살충제 사용 등 관리 알람 설정: AI가 식물 정보를 분석해 물 주기, 살충제 사용 등 관리 주기를 자동으로 계산하여 알림을 제공합니다.
- 알람 리스트 확인 및 On/Off 제어: 환경 설정 화면서 제공되는 토글 버튼을 통해 각 알람을 쉽게 활성화하거나 비활성화할 수 있습니다.

### 🌤️ 3. 날씨 기반 서비스
- 오늘의 지역 날씨 정보 제공: 날씨 API를 통해 사용자의 현재 위치 기반 날씨 정보를 실시간으로 가져와 식물 관리에 참고할 수 있도록 제공합니다.

### 🔍 4. 식물 추천 및 검색
- 사용자 설문조사 기반 식물 추천: 앱 최초 실행 시 진행하는 사용자 설문조사 결과를 기반으로, AI가 사용자의 환경과 선호에 맞는 식물을 자동 추천합니다. 추천된 식물은 검색 화면에서 확인할 수 있습니다.
- 날씨 기반 AI 추천: 현재 날씨 데이터를 바탕으로 AI가 오늘의 날에 적합한 식물을 추천합니다. 추천된 식물 역시 검색 화면에서 확인할 수 있습니다.
- 사진 업로드 -> 식물 자동 인식 및 검색: 사용자가 직접 업로드한 식물 사진을 AI 이미지 분석을 통해 인식하고, 해당 식물의 정보를 자동으로 가져옵니다. 분석된 식물은 저장하기 버튼을 눌러 다이어리에 바로 추가할 수 있습니다.

### 📒 5. 저장 및 관리 기능
- 식물 추가·삭제: 식물 추가는 사진 업로드 후 AI 분석을 통해 자동으로 정보를 불러와 저장할 수 있습니다. 식물 삭제는 다이어리 화면에서 특정 식물 카드를 2초 이상 길게 누른 다 삭제 버튼을 누르면 가능합니다.
- 식물 관리 기록 저장: 다이어리 화면의 식물 상세 페이지에서 다이어리 버튼을 누르면 식물 일지 화면으로 이동합니다. 여기서 식물 성장 기록, 관리 이력 등을 자유롭게 작성할 수 있습니다.
- 일정 관리 Todo List: 일정 화면에서 원하는 날짜를 선택한 뒤 + 버튼을 눌러 Todo를 등록할 수 있습니다. 식물을 선택하면 "식물명 - 해야 할 일" 형태로 저장, 식물을 선택하지 않으면 일반 할 일로 저장되어 직관적으로 일정과 관리를 함께 정리할 수 있습니다. 


## ⚙️ 개발 환경

| 항목 | 내용 |
|-----|------|
| 언어 | Kotlin |
| IDE | Android Studio |
| 빌드 도구 | Gradle |
| UI 프레임워크 | XML 기반 View System |
| AI 분석 | 이미지 분석 API, 날씨 API |
| 데이터베이스 | Room (SQLite 로컬 DB) |


## 📂 프로젝트 구조

```txt
.
├─app
│  │  build.gradle.kts
│  │  proguard-rules.pro
│  ├─build                  
│  └─src
│      ├─androidTest
│      │  └─java
│      │      └─com
│      │          └─Plant_application
│      │              └─ExampleInstrumentedTest.kt
│      ├─main
│      │  │  AndroidManifest.xml
│      │  ├─java
│      │  │  └─com
│      │  │      └─Plant_application
│      │  │          │  MainActivity.kt
│      │  │          │  PlantApplication.kt
│      │  │          ├─background
│      │  │          ├─data
│      │  │          │  ├─api
│      │  │          │  ├─database
│      │  │          │  ├─preference
│      │  │          │  └─repository
│      │  │          ├─ui
│      │  │          │  ├─add
│      │  │          │  ├─calendar     
│      │  │          │  ├─home    
│      │  │          │  ├─journal  
│      │  │          │  ├─mypage
│      │  │          │  ├─onboarding
│      │  │          │  └─search
│      │  │          ├─util
│      │  │          └─widget     
│      │  └─res
│      │      ├─anim
│      │      ├─color
│      │      ├─drawable
│      │      ├─layout
│      │      ├─menu
│      │      ├─mipmap-anydpi 
│      │      ├─mipmap-hdpi 
│      │      ├─mipmap-mdpi
│      │      ├─mipmap-xhdpi
│      │      ├─mipmap-xxhdpi
│      │      ├─mipmap-xxxhdpi   
│      │      ├─navigation     
│      │      ├─values      
│      │      ├─values-night      
│      │      └─xml
│      └─test
│          └─java
│              └─com
│                  └─Plant_application       
└─gradle
```

## 🧑‍💻 역할 분담
- 2171297 유예현 [(Doctor-238)](https://github.com/Doctor-238/)
  - 기획 / 기능 설계
  - 백엔드 연동, 로컬 데이터 처리

- 2171318 정연우 [(dusdb)](https://github.com/dusdb)
  - 프론트엔드 개발
  - 문서 작성
 
- 2171416 장승환 [(hsu-2171416-jangseunghwan)](https://github.com/hsu-2171416-jangseunghwan)
  - 프론트엔드 개발
  - 문서 작성

- 2371338학번 김필중 [(KPJ0616)](https://github.com/KPJ0616)
  - 프론트엔드 개발
  - UI 디자인

## 🎨 디자인 원본 (Figma)
- https://www.figma.com/design/ZUYdsAmz7DyQVXqUUdHOGL/Untitled?node-id=0-1&t=U4Xlle57Gn0C6SOp-1

