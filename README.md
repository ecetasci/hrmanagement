HR Management

Bu proje, Spring Boot ve React ile geliştirilen bir insan kaynakları yönetimi sistemidir.
Projede şirket onaylama, personel yönetimi, izin talepleri, tanımlamalar, e‑posta bildirimi ve JWT tabanlı kimlik doğrulama gibi işlevler yer alır.

Kurulum Rehberi
Önkoşullar

Java 17: Uygulama Spring Boot 3 kullandığı için JDK 17 veya üzeri gereklidir.

PostgreSQL 10+ veritabanı sunucusu.

Gradle: Proje, kendi gradlew betiğiyle gelir; sisteminizde ek kuruluma gerek yoktur.

Git: Kaynak kodunu klonlamak için.

Lombok eklentisi: Projedeki anotasyonların IDE tarafından doğru şekilde derlenmesi için IDE’nize Lombok eklentisi kurulmalıdır.

Adımlar

Kaynak kodunu klonlayın ve proje dizinine gidin:

git clone <repository-url>
cd hrmanagement-master/hrmanagement-master


Veritabanını oluşturun:

Uygulamanın varsayılan yapılandırmasında jdbc:postgresql://localhost:5432/hr_management adresine bağlanılır. PostgreSQL’de aşağıdaki şekilde bir veritabanı ve kullanıcı oluşturabilirsiniz. Kullanıcı adı ve parola uygulamanın konfigürasyonuna göre değiştirebilirsiniz.

-- PostgreSQL terminalinde
CREATE DATABASE hr_management;
CREATE USER postgres PASSWORD 'root';
GRANT ALL PRIVILEGES ON DATABASE hr_management TO postgres;


Varsayılan bağlantı bilgileri src/main/resources/application.yml dosyasında tanımlanmıştır. Dosyada spring.datasource.url, spring.datasource.username ve spring.datasource.password değerlerini kendi ortamınıza göre düzenleyebilirsiniz.

Ortam değişkenlerini ayarlayın:

JWT_SECRET: Uygulama, JWT token’larını imzalamak için app.jwtSecret özelliği üzerinden bir gizli anahtar kullanır. Bu anahtarı bir ortam değişkeni olarak tanımlayın ve uygulama başlatılırken yüklenmesini sağlayın:

export JWT_SECRET="super-secret-key"


Dosya yükleme dizini ve e‑posta ayarları: application.yml dosyasında file.upload-dir alanı yüklenen dosyaların kaydedileceği dizini belirtir. SMTP üzerinden e‑posta gönderebilmek için spring.mail.host, spring.mail.username ve spring.mail.password gibi alanları doldurmanız gerekir. Varsayılan ayarlar Gmail SMTP sunucusuna göre düzenlenmiştir.

Projeyi derleyip çalıştırın:

Gradle wrapper ile projeyi derleyebilir ve çalıştırabilirsiniz:

# Bağımlılıkları indirip uygulamayı derler
./gradlew build

# Uygulamayı başlatır (varsayılan port 8080)
./gradlew bootRun


Uygulama başlatıldıktan sonra http://localhost:8080/swagger-ui/index.html
 adresindeki Swagger UI arayüzünden tüm REST servislerini test edebilirsiniz. Swagger arayüzü sayesinde örnek istek gövdeleri ve cevapları görülebilir.

Veritabanı şeması ve başlangıç verileri:

Spring JPA ayarı olarak spring.jpa.hibernate.ddl-auto=update etkin olduğu için veritabanı şeması uygulama ilk çalıştığında otomatik olarak oluşturulur. DatabaseSeeder sınıfı (development profilinde) başlangıç veri seti ve ilk site yöneticisi kullanıcısı ekler.

Database Setup

Bu proje, PostgreSQL veritabanı kullanır. Varsayılan konfigürasyonda veritabanı adı hr_management, kullanıcı adı postgres ve parola root olarak tanımlanmıştır.
Eğer farklı bir veritabanı veya kullanıcı kullanacaksanız, src/main/resources/application.yml dosyasındaki spring.datasource.* alanlarını değiştirin ve aşağıdaki komutlarla veritabanını oluşturun:

-- Örnek olarak kendi kullanıcı adınızı ve parolanızı kullanın
CREATE DATABASE hr_management;
CREATE USER myuser PASSWORD 'mypassword';
GRANT ALL PRIVILEGES ON DATABASE hr_management TO myuser;

Environment Configuration

Uygulamanın çalışması için gerekli başlıca konfigürasyonlar application.yml dosyasında yer alır:

Veritabanı: spring.datasource.url, spring.datasource.username, spring.datasource.password alanları PostgreSQL bağlantısını tanımlar.

JWT gizli anahtarı: app.jwtSecret: ${JWT_SECRET} tanımı, JWT anahtarını ortam değişkeninden okur. Uygulama öncesinde JWT_SECRET değişkenini tanımlamalısınız.

Mail sunucusu: spring.mail.host, spring.mail.username, spring.mail.password alanları e‑posta gönderimi için SMTP konfigürasyonunu belirtir. Gerekli olduğunda bu değerleri doldurun.

Dosya yükleme dizini: file.upload-dir alanı, yüklenen dokümanların kaydedileceği klasörü gösterir.

Ortam değişkenlerini bir .env dosyasında veya işletim sistemi düzeyinde ayarlayabilirsiniz. Örneğin:

export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="root"
export JWT_SECRET="super-secret-key"


Uygulama, server.port özelliği üzerinden farklı bir portta çalıştırılabilir. Varsayılan port 8080’dir.

API Kullanım Örnekleri
Kimlik Doğrulama API’leri

Kimlik doğrulama ve kayıt işlemleri /api/auth altında tanımlıdır:

HTTP Yöntemi	URL	Açıklama
POST	/api/auth/register	Şirket yöneticisi kayıt eder. İstek gövdesi, şirket bilgileri ve yönetici kullanıcı bilgilerini içerir.
POST	/api/auth/login	Kullanıcı girişi yapar ve JWT token döner.
POST	/api/auth/verify-email	E‑posta doğrulama işlemini tamamlar.
POST	/api/auth/forgot-password	Şifre sıfırlama talebi oluşturur.
POST	/api/auth/reset-password	Şifreyi yeniler.
POST	/api/auth/logout	Aktif oturumu sonlandırır.

Giriş işlemi örneği:

curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@example.com","password":"yourPassword"}'


Yanıt başarıysa, aşağıdaki gibi bir JWT token alırsınız. Bu token’ı korumalı isteklere Authorization: Bearer <token> başlığıyla eklemeniz gerekir.

{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}

Yönetici API’leri (Site Admin)

Site yöneticisi şirket başvurularını yönetebilir:

HTTP Yöntemi	URL	Açıklama
GET	/api/admin/companies	Şirket başvurularının listesini getirir.
PUT	/api/admin/companies/{id}/approve	Belirtilen şirket başvurusunu onaylar.
PUT	/api/admin/companies/{id}/reject	Belirtilen başvuruyu reddeder.
POST	/api/admin/companies/{id}/subscription	Şirket için üyelik planı oluşturur (Aylık/Yıllık).

Her çağrı, Authorization: Bearer <token> başlığında site yöneticisi yetkisine sahip bir kullanıcı token’ı gerektirir. Örnek şirket onaylama isteği:

curl -X PUT http://localhost:8080/api/admin/companies/5/approve \
     -H "Authorization: Bearer <your-admin-token>"

Şirket Yöneticisi API’leri

Şirket yöneticileri kendi şirketlerindeki personeli ve izin tanımlarını yönetebilir:

HTTP Yöntemi	URL	Açıklama
GET	/api/company/employees	Personel listesini getirir.
POST	/api/company/employees	Yeni personel ekler.
PUT	/api/company/employees/{id}	Personeli günceller.
DELETE	/api/company/employees/{id}	Personeli siler.
PUT	/api/company/employees/{id}/activate	Personeli aktifleştirir veya pasifleştirir.

Örnek: yeni bir çalışan eklemek için şirket yöneticisi yetkisine sahip token ile POST isteği atabilirsiniz.

curl -X POST http://localhost:8080/api/company/employees \
     -H "Authorization: Bearer <company-admin-token>" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "Ayşe Yılmaz",
       "email": "ayse@example.com",
       "position": "Developer",
       "department": "IT",
       "salary": 10000
     }'

İzin Yönetimi API’leri

Çalışanlar izin talebi oluşturabilir, şirket yöneticileri de izinleri onaylayabilir veya reddedebilir:

HTTP Yöntemi	URL	Açıklama
GET	/api/company/leaves	Şirket içindeki tüm izin taleplerini listeler.
POST	/api/employee/leaves	Çalışan için yeni bir izin talebi oluşturur.
PUT	/api/company/leaves/{id}/approve	Belirli bir izin talebini onaylar.
PUT	/api/company/leaves/{id}/reject	Belirli bir izin talebini reddeder.

Örnek izin talebi isteği:

curl -X POST http://localhost:8080/api/employee/leaves \
     -H "Authorization: Bearer <employee-token>" \
     -H "Content-Type: application/json" \
     -d '{
       "leaveTypeId": 3,
       "startDate": "2025-11-01",
       "endDate": "2025-11-03",
       "employeeNote": "Sağlık nedeni ile"
     }'

Ek Bilgiler

Swagger UI: Uygulama çalıştığında http://localhost:8080/swagger-ui/index.html adresinden API uç noktalarını inceleyebilir ve test çağrıları yapabilirsiniz.

JWT Tabanlı Güvenlik: Tüm korumalı API çağrılarında Authorization: Bearer <jwt-token> başlığı zorunludur.

Roller: Uygulamada SITE_ADMIN, COMPANY_ADMIN ve EMPLOYEE olmak üzere üç temel rol bulunur. Güvenlik yapılandırması, her rolün erişebileceği URI desenlerini belirler (bkz. SecurityConfig).

Hata Yönetimi: Proje, özelleştirilmiş BusinessException, ResourceNotFoundException gibi istisnalar ve GlobalExceptionHandler sınıfı ile standart hata yanıt formatı sağlar.

Seeder: Geliştirme profilinde uygulama açılışında örnek veriler ve ilk site yöneticisi kullanıcısı otomatik olarak eklenir.

Bu README, projeyi hızlıca kurmanız ve temel API çağrılarını yapmanız için bir rehber niteliğindedir.
