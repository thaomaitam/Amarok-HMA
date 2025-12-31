## Phân Tích Nguyên Lý và Logic Hoạt Động của Amarok

### 1. Tổng Quan Kiến Trúc

Amarok là ứng dụng Android có chức năng ẩn file và ứng dụng. Ứng dụng hoạt động trên 3 trạng thái chính được quản lý bởi lớp `Hider`:

```
HIDDEN → PROCESSING → VISIBLE
```

---

### 2. Core Logic - Hider.java

**Luồng xử lý chính:**

```
hide(context) → tryToActivate() → processHide() → [AppHider.hide() + FileHider.hide()]
unhide(context) → tryToActivate() → processUnhide() → [AppHider.unhide() + FileHider.unhide()]
```

**Đặc điểm:**
- Sử dụng `HandlerThread` riêng để xử lý bất đồng bộ
- `MutableLiveData<State>` để theo dõi và broadcast trạng thái
- Observer pattern để đồng bộ trạng thái với SharedPreferences

---

### 3. File Hiding - 2 Phương Thức

#### 3.1 ObfuscateFileHider (Không cần Root)
**Nguyên lý:**
1. **Mã hóa tên file:** `filename` → `.Base64(filename)!amk`
2. **Xử lý header (8 bytes đầu):** `bytes[i] = ~bytes[i]` (bit-flip)
3. **Xử lý toàn bộ file text:** Bit-flip từng byte trong buffer 1KB

**Marking system:**
- `!amk` - Chỉ rename
- `!amk1` - Full process (rename + encrypt whole file)
- `!amk2` - Header process (rename + encrypt 8 bytes đầu)

#### 3.2 ChmodFileHider (Yêu cầu Root)
**Nguyên lý:**
```bash
chmod -R 0 <path>     # Hide: Xóa toàn bộ quyền
chmod -R 2770 <path>  # Unhide: Khôi phục quyền
```

---

### 4. App Hiding - 4 Phương Thức

#### 4.1 RootAppHider
```bash
# Hide
pm disable <package>
pm hide <package>

# Unhide  
pm unhide <package>
pm enable <package>
```

#### 4.2 ShizukuAppHider
Sử dụng reflection để gọi API hệ thống thông qua Shizuku binder:
- `IPackageManager.setApplicationEnabledSetting()` - Disable/Enable app
- `IPackageManager.setApplicationHiddenSettingAsUser()` - Hide/Unhide từ danh sách apps

#### 4.3 DhizukuAppHider & DsmAppHider
Tương tự Shizuku, sử dụng Device Owner/Device Admin APIs.

---

### 5. XHide Module (Xposed)

**Mục đích:** Lọc ứng dụng khỏi các truy vấn PackageManager ở mức hệ thống.

**Kiến trúc:**

```
Main App                    Xposed Module
    │                           │
    ▼                           ▼
XHidePrefBridge ─────────► XPref (Shared)
    │                           │
    │   commitNewValues()       │
    └──────────────────────────►│
                                │
                                ▼
                         FilterHooks
                                │
                                ▼
                    PackageManagerService
                    (hook getInstalledApplications,
                     getInstalledPackages)
```

**Logic lọc (FilterUtils):**
```java
// Khi app gọi getInstalledPackages()
result = original_method()
return result.filter(pkg -> !XPref.shouldHide(pkg.packageName))
```

**Tương thích Android:**
- **Android 13+ (API 33):** Hook `IPackageManagerBase`
- **Android 10-12 (API 29-31):** Hook `PackageManagerService`
- **Android cũ hơn:** Legacy hooks

---

### 6. Quick Hide Triggers

- **QSTileService:** Quick Settings tile
- **QuickHideService:** Foreground service với panic button nổi
- **ScreenStatusReceiver:** Tự động ẩn khi tắt màn hình
- **ActionReceiver:** Intent API
- **ToggleWidget:** Home screen widget

---

### 7. Flow Diagram Tổng Hợp

```
User Trigger (Tile/Button/Intent)
           │
           ▼
    Hider.hide(context)
           │
           ▼
    AppHider.tryToActivate()
           │
     ┌─────┴─────┐
     │           │
   Success     Failure
     │           │
     ▼           ▼
processHide()  Show Error
     │
     ├──► AppHider.hide() ──► pm disable/hide
     │                         hoặc Shizuku API
     │
     └──► FileHider.hide() ──► Obfuscate/Chmod
                │
                ▼
    XHidePrefBridge.commitNewValues()
                │
                ▼
         XPref (Xposed)
                │
                ▼
    FilterHooks intercept PackageManager
```