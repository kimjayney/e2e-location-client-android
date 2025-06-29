# Location Tracker Android App

실시간 위치 추적 Android 애플리케이션입니다.

## 기능

- 실시간 위치 추적
- AES 암호화를 통한 위치 데이터 보안
- 서버로 위치 데이터 전송
- 배터리 절약 모드
- 루팅 감지 및 보안 경고
- 다국어 지원 (한국어/영어)

## 설치 및 빌드

### 1. 저장소 클론
```bash
git clone https://github.com/your-username/e2e-location-client-android.git
cd e2e-location-client-android
```

### 2. 환경 설정
`.env.example` 파일을 `.env`로 복사하고 필요한 정보를 입력하세요:

```bash
cp .env.example .env
```

`.env` 파일에서 다음 정보를 설정하세요:
```bash
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
SERVER_URL=https://your-server-url.com
WEB_URL=https://your-web-url.com
APP_VERSION=1.0.0
APP_VERSION_CODE=1
```

### 3. 서명 키 생성
릴리즈 빌드를 위해 서명 키를 생성하세요:

```bash
mkdir -p app/keystore
keytool -genkey -v -keystore app/keystore/release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias your_key_alias \
  -storepass your_keystore_password \
  -keypass your_key_password \
  -dname "CN=Your Name, OU=Your Organization, O=Your Company, L=Your City, S=Your State, C=Your Country"
```

### 4. 빌드

#### 디버그 빌드
```bash
./gradlew assembleDebug
```

#### 릴리즈 빌드
```bash
./gradlew assembleRelease
```

## 보안 고려사항

- **루팅 감지**: 루팅된 기기에서는 앱이 실행되지 않습니다
- **데이터 암호화**: 모든 위치 데이터는 AES 암호화되어 전송됩니다
- **권한 최소화**: 필요한 위치 권한만 요청합니다

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 주의사항

- `.env` 파일과 `app/keystore/` 디렉토리는 절대 Git에 커밋하지 마세요
- 릴리즈 APK 파일도 Git에 포함하지 마세요
- 서명 키는 안전한 곳에 백업해두세요