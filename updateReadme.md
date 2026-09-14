# Cách cập nhật ảnh cho README.md

Tài liệu vận hành cho việc chụp lại ảnh/GIF trong [`README.md`](README.md). Đọc file này **trước
khi** đụng vào ảnh trong [`docs/images/`](docs/images/).

Câu hỏi file này trả lời:

- Đổi UI ở `HomeFragment.kt` hay `HomeView.swift` thì phải chụp lại **những capture nào**?
- Chỗ này nên là **ảnh tĩnh hay GIF**?
- Vào màn đó bằng **thao tác nào**, và dùng **item nội dung nào**?
- **Cột iOS** trong mỗi bảng lấy ảnh ở đâu ra?
- Làm màn hình mới thì thêm capture kiểu gì?

> **Đây là tài liệu sống.** Thêm màn hình, thêm section, đổi thứ tự dummy data, hay đổi đường đi vào
> một màn — đều phải cập nhật file này trong **cùng một change**. Một bảng ánh xạ sai còn tệ hơn
> không có bảng, vì nó khiến người sau tin là mình đã chụp đủ.

---

## 1. Vì sao mỗi mục đều là bảng hai cột

Đây là dự án **Kotlin Multiplatform**: `shared` giữ domain, data và ViewModel; Android render bằng
XML/`RecyclerView`, iOS render bằng SwiftUI. Toàn bộ luận điểm của repo nằm ở chỗ **một
`StateFlow` duy nhất nuôi hai UI native khác nhau**.

Một README chỉ có ảnh Android đang chứng minh điều ngược lại. Vì vậy mọi mục ảnh trong README là
một bảng hai cột:

| Cột | Nguồn | Tự động? |
|---|---|---|
| **Android** | `tools/capture_media.py` qua `adb` | Có, lặp lại được |
| **iOS** | `xcrun simctl` quay màn hình, **người thao tác bằng tay** | Nửa vời — công cụ lo phần quay, người lo phần chạm |

`simctl` **không có lệnh chạm màn hình**. Nó chụp và quay được simulator nhưng không bấm được vào
đó, nên cột iOS vĩnh viễn không tự động hóa được bằng công cụ này. Điều công cụ vẫn làm là phần sau
cử chỉ: cùng fps, cùng chiều rộng, cùng bảng màu, cùng tên file như cột Android — để hai ô của một
hàng thực sự so sánh được với nhau.

### Ảnh iOS được lồng trong khung máy

`simctl` trả về đúng framebuffer: một hình chữ nhật 1170 × 2532 góc vuông. Cạnh một ảnh Android thì
nó trông như ảnh của **một cái màn hình**, không phải ảnh của **một cái máy**. Vì vậy mọi capture iOS
được [`tools/device_frame.py`](tools/device_frame.py) lồng vào thân máy trước khi ghi ra file.

Hình dáng màn hình **không phải vẽ tay**. Mỗi bundle `.simdevicetype` có sẵn một PDF `framebufferMask`
đúng bằng kích thước framebuffer, phần đục của nó chính là vùng màn hình nhìn thấy được — bo góc, và
với máy có tai thỏ thì khoét luôn phần tai. Thân máy suy ra từ mask đó bằng phép giãn nở, nên viền dày
đều nhau ở mọi chỗ kể cả bốn góc, bất kể Apple dùng đường cong nào.

Đổi simulator thì sửa đúng một hằng số `IOS_DEVICE` trong `capture_media.py`. Xem thử khung máy:

```bash
python3 tools/device_frame.py "iPhone 16e" /tmp/frame.png
```

Nền quanh thân máy là **màu đặc**, không trong suốt. GIF lưu mỗi khung hình bằng những điểm ảnh khác
khung trước và đánh dấu phần còn lại là trong suốt; giữ một ô bảng màu cho nền trong suốt là lấy mất
cơ chế đó và mọi khung hình bị ghi nguyên vẹn. Trên một cảnh cuộn chín giây, khác biệt đo được là
**4 MB so với 1 MB**.

```bash
python3 tools/capture_media.py ios player-mini
```

Lệnh này in ra **đúng cử chỉ cần làm** (trường `ios_steps` của capture đó), đếm ngược, quay đủ số
giây, rồi chuyển sang GIF. Việc của người chạy là thao tác trên simulator trong lúc nó quay.

**Tên file là hợp đồng giữa hai cột.** Mọi file đều có hậu tố nền tảng:

| Android | iOS |
|---|---|
| `docs/images/player-mini-android.gif` | `docs/images/player-mini-ios.gif` |
| `docs/images/home-overview-android.webp` | `docs/images/home-overview-ios.webp` |

Chụp xong cột iOS thì thay ô `<em>đang chờ<br><code>…</code></em>` trong README bằng thẻ `<img>`
tương ứng. Không phải sửa đường dẫn nào khác.

---

## 2. Chuẩn bị

### Android

```bash
adb devices                      # phải thấy ĐÚNG MỘT device
ffmpeg -version                  # cần cho cả WebP lẫn GIF
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell settings put secure immersive_mode_confirmations confirmed
```

Dòng cuối **bắt buộc** với StoryGroup và player fullscreen: hai màn đó ẩn system bar, và lần đầu
vào Android sẽ phủ lên một tấm thẻ trắng *"Viewing full screen — to exit, swipe down"*. Nó là
system overlay nên vẫn nằm trong ảnh chụp, và trông y như một cái dialog của app.

Emulator dùng để chụp bộ ảnh hiện tại: **Pixel (1080 × 2400, density 420), API 37, `-gpu auto`**.

### iOS

```bash
xcrun simctl list devices booted    # phải thấy ĐÚNG MỘT simulator
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 16e' build
xcrun simctl install booted <DerivedData>/Build/Products/Debug-iphonesimulator/StreamTV.app
xcrun simctl launch booted com.congnguyencn.kmpstreamtv.StreamTV
```

Simulator dùng để chụp bộ ảnh hiện tại: **iPhone 16e, iOS 26.2 (1170 × 2532)**.

Nếu công cụ điều khiển simulator báo *"Xcode is installed but not selected"* thì chạy một lần:

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

`xcode-select -p` có thể đã trả về đúng đường dẫn nhờ giá trị mặc định, nhưng file
`/var/db/xcode_select_link` vẫn chưa tồn tại — và đó mới là thứ công cụ kiểm tra.

### Lệnh

```bash
python3 tools/capture_media.py list              # xem toàn bộ capture và loại của nó
python3 tools/capture_media.py shot <tên>        # chụp một ảnh tĩnh Android
python3 tools/capture_media.py gif <tên>         # quay một GIF Android
python3 tools/capture_media.py all               # chụp lại toàn bộ Android (~15 phút)
python3 tools/capture_media.py ios <tên>         # quay cột iOS, người thao tác bằng tay
python3 tools/capture_media.py ios <tên> --from <file.mov> --trim 5.2:2.6,10.5:2.3
```

Hai cờ cuối dành riêng cho cột iOS, và tồn tại vì người thao tác là **người**:

- **`--from <file.mov>`** mã hoá lại một bản quay đã có thay vì bắt diễn lại cử chỉ. Khi GIF vượt
  ngân sách kích thước, công cụ **giữ lại** file `.mov` và in sẵn câu lệnh này — hạ `ios_gif_*` rồi
  chạy lại, không phải quay lại. (`docs/images/*.mov` đã nằm trong `.gitignore`.)
- **`--trim START:GIÂY[,START:GIÂY…]`** giữ lại vài lát cắt của bản quay rồi nối chúng. Bản quay
  còn chứa cả lúc với tay lấy chuột, cú vuốt hụt, và khoảng chờ giữa hai bước. Giữ bốn khoảnh khắc
  đáng giá và bỏ phần ở giữa **không hề làm sai lệch** — mỗi lát là footage nguyên vẹn — nhưng là
  khác biệt giữa một GIF sáu giây về hành vi và một GIF hai mươi giây về việc ai đó đang bấm máy.

**Không chạy `all` khi chỉ sửa một màn.** Nó mất ~15 phút và tạo diff rác trên những ảnh không liên
quan (video đang phát ở frame khác nhau). Chỉ chạy `all` khi đổi token/theme dùng chung.

---

## 3. Công cụ nhắm mục tiêu bằng gì

`capture_media.py` **không đếm toạ độ pixel**. Nó đọc cây accessibility bằng `uiautomator` và nhắm
theo `content-desc` hoặc `resource-id`:

```python
"tap:desc=Pulse of the court"      # card trong carousel/rail
"tap:id=shorts"                    # item bottom navigation
"scrollto:desc=Grace in every gesture"
"wait:text=Recommended for you"
```

Toạ độ cứng sống được đúng tới lần đổi spacing đầu tiên, rồi sau đó **âm thầm chụp nhầm card** mà
vẫn ra một tấm ảnh hợp lệ. `content-desc` thì chính là chuỗi mà lớp accessibility đã bảo đảm.

Hai chỗ buộc phải dùng toạ độ (dạng **phân số** của màn hình, không phải pixel):

1. **Bên trong player.** Video là `SurfaceView`, và `uiautomator dump` **từ chối trả về cây** khi
   surface đang render — nó báo window busy. Nút của controller vì thế nằm trong hằng số
   `PLAYER_PIP`, `PLAYER_QUALITY`, `PLAYER_EMPTY_SURFACE`. Đổi layout controller thì **đo lại** từ
   một ảnh `player-detail`.
2. **Bên trong story.** Nửa trái/nửa phải của khung là vùng chạm, không có node riêng.

`find_node` chọn node **rộng nhất** khớp, không phải node đầu tiên: cùng một `content-desc` xuất
hiện cả trên card đã layout đủ lẫn trên mẩu vài chục pixel của card bên cạnh ló ra ở mép carousel.
Chạm vào mẩu đó mở nhầm item và vẫn ra ảnh trông hợp lệ.

---

## 4. Ảnh tĩnh hay GIF?

Quy tắc duy nhất: **thứ cần chứng minh có nằm ở sự thay đổi giữa các frame không?**

| Dùng | Khi điều cần nói là | Ví dụ trong README |
|---|---|---|
| **Ảnh tĩnh** (`shot`) | Bố cục, thứ bậc thị giác, màu, hoặc **một trạng thái cuối** | `player-detail`, `short-actions`, `player-settings` |
| **GIF** (`gif`) | Cử chỉ, animation, hoặc **quan hệ nhân quả giữa hai trạng thái** | `player-mini`, `player-pip`, `story-viewer` |

GIF đắt hơn nhiều: vài trăm KB đến hơn 1 MB mỗi cái so với 40–80 KB cho ảnh tĩnh. Chỉ dùng GIF khi
một ảnh tĩnh thực sự **không thể** nói được điều đó. Những cái đang xứng đáng:

- **`player-mini`** — điểm mấu chốt là *player co lại thành thẻ nổi rồi bung ra lại*. Không frame
  đơn lẻ nào diễn tả được quan hệ giữa detail screen và mini card.
- **`player-pip`** — hand-off từ app sang cửa sổ hệ thống. Ảnh tĩnh chỉ cho thấy một cửa sổ nhỏ,
  không cho thấy nó **đến từ đâu**.
- **`player-mini-corner`** — thẻ đỗ vào góc nào là do thả ở đâu; đó là chuyển động.
- **`story-viewer`** — reaction bay lên và thanh progress chạy.
- **`short-feed`** — trao playback giữa các page khi vuốt.
- **`home-feed` / `home-categories`** — cuộn, và thanh category thu lại theo cuộn.

Ngược lại: "panel Settings mở ra" là **trạng thái**, không phải chuyển động → ảnh tĩnh.

---

## 4b. Ba giới hạn của iOS Simulator

Ba ô trong README **không thể** chụp từ simulator, và không phải vì app thiếu tính năng.

| Giới hạn | Hệ quả | Cách lấy ảnh |
|---|---|---|
| `AVPictureInPictureController.isPictureInPictureSupported()` trả **`false`** trên Simulator | `PlayerControllerView` chỉ vẽ nút PiP khi `coordinator.isSupported`, nên trên simulator **không có nút PiP** — app đang làm đúng | `player-pip-ios` phải chụp từ **máy thật** |
| `simctl` **không xoay** được simulator, và cũng không có verb nào cho việc đó | Bấm nút fullscreen trong controller không làm màn hình xoay | `player-fullscreen-ios` phải xoay tay (Device → Rotate) hoặc chụp từ máy thật |
| `simctl` **không chạm** được màn hình | Toàn bộ cột iOS không tự động hóa được bằng `simctl` một mình | Xem mục 1 |

### Card story trên iOS chỉ mở bằng avatar

Trên Android, chạm bất kỳ đâu trên card story đều mở story viewer. Trên iOS **chỉ vùng avatar của
provider** (góc trên bên trái card) mở được; chạm vào giữa card không có tác dụng gì cả — không mở
story, không mở short, không báo lỗi.

Đây là **lệch hành vi giữa hai nền tảng**, không phải quirk của simulator: `StorySectionView` bọc
`HomeContentCard(style: .story)` trong `Button` với `.buttonStyle(.plain)`, mà card đó là một
`RemoteArtwork` có `.clipShape(...)` và không khai báo `.contentShape(Rectangle())`, nên vùng nhận
chạm co lại chỉ còn phần thực sự vẽ ra — và ở đây phần đó là cái avatar overlay.

Cho tới khi sửa: capture `story-viewer-ios` và `story-hold-ios` **phải chạm vào avatar**, và
`ios_steps` của hai capture đó nói đúng như vậy. Sửa xong thì nhớ cập nhật lại `ios_steps`.

---

## 5. Bẫy lớn nhất: decoder của emulator làm hỏng H.264 Main/High

Đây là thứ làm hỏng capture nhiều nhất, và nó **không phải bug của app**.

Decoder H.264 của emulator render đúng **Baseline profile**, còn **Main và High** thì ra các dải
nhiễu màu — xanh lá, hồng, tím. Trong lúc đó `position`, `duration` và trạng thái buffering vẫn báo
hoàn toàn bình thường, nên **không có gì trong app hay trong công cụ nhận ra**. Capture chạy xong,
file hợp lệ, độ dài đúng — chỉ có mở ra nhìn mới biết.

Đối chiếu ladder ABR của từng stream trong
[`HomeDummyDataSource.kt`](shared/src/commonMain/kotlin/com/congnguyencn/kmpstreamtv/feature/home/data/source/HomeDummyDataSource.kt):

| Stream | Rendition | Profile | Trên emulator |
|---|---|---|---|
| `APPLE_TS` / `APPLE_FMP4` (bipbop) | 416×234 → 1920×1080 | Baseline | **Sạch** |
| `JW_BUNNY` (dùng cho short/story) | một rendition | Baseline | **Sạch** |
| `BIG_BUCK_BUNNY` | 320×184, 512×288 Baseline; **848×480, 1280×720 High** | hỗn hợp | Nhiễu khi ABR leo lên High |
| `TEARS_OF_STEEL` | 224×100, 448×200 Baseline; **784×350 Main**; 1680×750 High | hỗn hợp | Nhiễu |
| `SINTEL` | — | — | Host trả **403**; app hiện đúng màn not-entitled |

**Vì vậy mọi demo player dùng `Pulse of the court`** (`video-basketball`, stream `APPLE_TS`). Đó là
card đầu của carousel *Featured today*, chạm được ngay khi mở app, không cần cuộn — và là fixture
duy nhất mà cả ladder đều Baseline. Nội dung là test pattern của Apple, hơi khô, nhưng nó hiện
timecode và phụ đề, tức là đọc được ngay controller đang ở trạng thái nào.

### Đã thử và **không** giải quyết được

| Cách | Kết quả |
|---|---|
| `service call SurfaceFlinger 1008 i32 1` (tắt hardware overlay) | Không đổi — lỗi nằm ở decode, không phải composite |
| `-gpu swiftshader_indirect` | Không đổi |
| `setprop debug.stagefright.ccodec 0` | Không đổi (cần restart mediaserver) |
| `adb emu network speed umts` để ép ABR xuống Baseline | **Có tác dụng**, nhưng ExoPlayer mở bằng ước lượng mặc định 1 Mbit nên vẫn bắt đầu ở rendition High và mất **hơn 20 giây** mới hạ xuống. Chụp ở giây thứ 20 ra ảnh sạch của một video hỏng |

Nếu sau này đổi fixture của player sang stream khác, **mở ảnh ra nhìn**, đừng tin là xong vì lệnh
chạy không lỗi.

---

## 6. Bẫy thứ hai: controller tự ẩn sau 5 giây, nhưng chỉ khi đang phát

`PlayerView.scheduleControllerHide()` chỉ hẹn giờ ẩn khi `latestState.isPlaying` đúng. Trên emulator
hay rebuffer, `isPlaying` nhấp nháy, nên **không đoán được** lúc chụp controller đang hiện hay ẩn.
Một kịch bản "chạm để hiện controller rồi chụp" vì thế có 50% khả năng **chạm để ẩn nó đi**.

Cách làm đúng, đã áp dụng trong `SHOW_CONTROLLER`: **chạm đúng một lần vào vị trí nút play/pause.**

- Controller đang ẩn → nút có `isVisible = false`, chạm rơi vào `viewMask` → controller hiện ra.
- Controller đang hiện → chạm trúng `playPauseAction` → pause, mà pause thì huỷ luôn hẹn giờ ẩn.

Cả hai nhánh đều kết thúc với **controller đang hiện**, là tính chất duy nhất mấy capture này cần.
Đổi lại, biểu tượng play/pause có thể ngả về bên nào cũng được — chấp nhận.

---

## 7. Dùng item nội dung nào

| Demo | Item | Stream | Đường tới |
|---|---|---|---|
| Player (mọi capture) | `video-basketball` — *Pulse of the court* | `APPLE_TS` | Carousel *Featured today*, card đang ở giữa |
| Story | `short-cricket` — *Before the strike* | `JW_BUNNY` | Rail *Stories for you*, card đầu |
| Short feed | `short-cricket` — *Before the strike* | `JW_BUNNY` | Bottom nav → **Short** |

### Ba cái bẫy về dummy data

1. **Rail *Videos for you* là `videos.reversed()`**, nên card đầu của nó là item **cuối** trong
   `videos` (`video-ceremony`, Big Buck Bunny). Đổi thứ tự `videos` là đổi luôn item mà rail đó mở.
2. **Rail *Fresh shorts* là `shorts.reversed()`** — cùng một cái bẫy.
3. **`trailerUrl` không đi cùng `videoUrl`.** Item có *video* là Big Buck Bunny không phải item có
   *trailer* là Big Buck Bunny. Hiện chưa có capture nào dùng trailer; nếu thêm thì phải đếm lại.

### Thứ tự section trong Home, từ trên xuống

| # | Section | viewType |
|---|---|---|
| 0 | Stories for you | `ShortsPopular` |
| 1 | Featured today | `Banner` |
| 2 | Videos for you | `Videos` |
| 3 | Popular videos | `VideosPopular` |
| 4 | Documentary series | `Series` |
| 5 | Live channels | `Channels` |
| 6 | Editor's spotlight | `VerticalBanner` |
| 7 | Continue watching | `ContinueWatching` |
| 8 | Fresh shorts | `Shorts` |
| 9 | Explore StreamTV | `MiniApps` |

**Bottom navigation, từ trái sang phải**: Home (`id/home`), Music (`id/tvcab`), Short (`id/shorts`),
Playlist (`id/playlist`). Lưu ý `id/tvcab` — id của tab Music **không** phải `music`.

### Controller của player, theo phân số màn hình

| Hằng số | Nút | Phân số |
|---|---|---|
| — | Minimize (chevron xuống) | `0.629, 0.0967` |
| `PLAYER_PIP` | Picture in Picture | `0.747, 0.0967` |
| — | Close | `0.911, 0.0967` |
| `SHOW_CONTROLLER` | Play / pause | `0.5, 0.184` |
| `PLAYER_QUALITY` | Settings (chất lượng) | `0.719, 0.242` |
| — | Fullscreen | `0.911, 0.242` |
| `PLAYER_EMPTY_SURFACE` | Vùng trống để bắt đầu kéo thu nhỏ | `0.2, 0.13` |

`MinimizableView` **từ chối** cử chỉ kéo bắt đầu trên một control, nên `PLAYER_EMPTY_SURFACE` phải
nằm ngoài cả hàng nút trên lẫn hàng nút giữa. Và phải là `drag` (chậm, ~1100 ms) chứ không phải
`swipe`: bộ nhận cử chỉ phân loại theo đường đi từng frame, một cú fling bị bỏ qua.

---

## 8. Danh sách capture hiện có

| Tên capture | Loại | Nội dung | Đường vào (sau khi mở app) |
|---|---|---|---|
| `home-overview` | shot | Rail story trên carousel featured | — |
| `home-feed` | **gif** | Cuộn qua đủ các view type | vuốt lên ×4 |
| `home-categories` | **gif** | Thanh category thu lại rồi hiện lại | vuốt lên, vuốt xuống |
| `story-viewer` | **gif** | Progress phân đoạn, reaction, chạm phải sang story sau | chạm card story đầu |
| `story-hold` | **gif** | Giữ khung thì playback dừng | chạm card story đầu, giữ 3s |
| `short-feed` | **gif** | Vuốt trang, playback trao sang page mới | bottom nav → Short, vuốt lên ×2 |
| `short-actions` | shot | Action rail: follow, like, comment, share, more | bottom nav → Short |
| `short-interactions` | **gif** | Like/follow lạc quan, chạm để pause | bottom nav → Short, chạm các nút |
| `player-detail` | shot | VOD detail dọc: player 16:9, actions, provider, đề xuất | chạm banner, `SHOW_CONTROLLER` |
| `player-detail-scroll` | **gif** | Danh sách detail cuộn dưới player đứng yên | ↑ rồi vuốt lên ×2 |
| `player-mini` | **gif** | Kéo xuống → mini card → chạm để bung lại | ↑ rồi `drag`, `tap` |
| `player-mini-corner` | **gif** | Thẻ đỗ vào góc gần chỗ thả nhất | ↑ rồi thu nhỏ, kéo sang góc khác |
| `player-pip` | **gif** | Nút PiP trao playback cho cửa sổ hệ thống | ↑ rồi `SHOW_CONTROLLER`, chạm PiP |
| `player-fullscreen` | shot | Fullscreen ngang với controller đầy đủ | ↑ rồi xoay ngang |
| `player-settings` | shot | Cột quality / speed / audio / subtitle | ↑ rồi `SHOW_CONTROLLER`, chạm settings |

Định nghĩa đầy đủ (kể cả `setup`, `settle`, `duration`, `ios_steps`, `ios_gif_*`) nằm trong dict
`CAPTURES` của [`tools/capture_media.py`](tools/capture_media.py).

---

## 9. Sửa file nào thì chụp lại capture nào

Cột trái là thứ vừa sửa, cột phải là **toàn bộ** capture cần chạy lại — **cả hai nền tảng**, vì một
thay đổi ở `shared` thì đổi cả hai cột, còn một thay đổi ở UI native chỉ đổi một cột.

### Player

| Sửa | Chụp lại |
|---|---|
| `feature/player/widget/PlayerView.kt` · `Feature/Player/PlayerControllerView.swift` | `player-detail`, `player-fullscreen`, `player-pip` |
| `feature/player/PlayerFragment.kt` · `Feature/Player/PlayerOverlayStore.swift` | toàn bộ capture `player-*` |
| `feature/player/miniplayer/**` · `Feature/Player/MinimizablePlayerContainer.swift`, `MinimizableState.swift` | `player-mini`, `player-mini-corner` |
| `feature/player/widget/MiniPlaybackControllerView.kt` · `Feature/Player/MiniPlaybackControllerView.swift` | `player-mini`, `player-mini-corner` |
| `feature/player/widget/PlayerSettingsView.kt` · `Feature/Player/PlayerSettingsSheet.swift` | `player-settings` |
| `feature/player/PlayerDetailAdapter.kt` · `Feature/Player/PlayerDetailList.swift` | `player-detail`, `player-detail-scroll` |
| `MainActivity.enterPlayerPictureInPicture` · `Feature/Player/PlayerSurfaceView.swift` | `player-pip` |

### Home, story, short

| Sửa | Chụp lại |
|---|---|
| `feature/home/HomeFragment.kt`, `HomeSectionAdapter.kt` · `Feature/Home/HomeView.swift` | `home-feed`, `home-overview` |
| `feature/home/HomeTabFragment.kt` · `Feature/Home/HomeTabView.swift` | `home-categories` **và mọi shot có top bar** |
| `core/ui/recyclerview/**` | `home-feed` + `home-overview` |
| `feature/story/StoryGroupFragment.kt` · `Feature/Story/StoryGroupView.swift` | `story-viewer`, `story-hold` |
| animation reaction (`R.anim.emotion_animation`) · `Feature/Story/StoryReactionStore.swift` | `story-viewer` |
| `feature/short/ShortMediaFragment.kt`, `ShortMediaAdapter.kt` · `Feature/Short/ShortMediaView.swift` | `short-feed`, `short-actions`, `short-interactions` |
| `MainActivity.showDestination` · `App/MainTabView.swift` | `home-feed`, `short-feed` (bottom nav nằm trong khung) |

### Thay đổi lan rộng

| Sửa | Chụp lại |
|---|---|
| `HomeDummyDataSource.kt`, `ShortDummyDataSource.kt` | `all`, **và** kiểm tra lại mọi bảng ở mục 7 |
| `shared/.../presentation/**` (đổi UI model) | `all`, cả hai nền tảng |
| `core/ui` token, màu, font · `Core/UI/**` | `all`, cả hai nền tảng |
| `scripts/sync_ios_android_assets.py` | mọi capture iOS |

Ví dụ cụ thể — **sửa UI của `MinimizableView.kt`**:

```bash
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
python3 tools/capture_media.py gif player-mini
python3 tools/capture_media.py gif player-mini-corner
```

Rồi mở [`README.md`](README.md) mục *Mini player*, đọc lại caption dưới hai GIF đó xem còn đúng
không. Tên file không đổi nên không phải sửa đường dẫn.

---

## 10. Làm một màn hình mới

Năm bước, làm hết trong cùng một change:

1. **Thêm entry vào `CAPTURES`** trong [`tools/capture_media.py`](tools/capture_media.py). Đặt tên
   kebab-case theo dạng `<màn>-<thứ-cần-nói>`, ví dụ `profile-overview`. Tách `setup` (đường đi tới
   nơi, không quay) khỏi `steps` (chính phần biểu diễn) — GIF chỉ nên chứa phần biểu diễn.
2. **Nếu là GIF thì thêm tên vào `GIF_CAPTURES`.** Không thêm thì nó bị chụp thành ảnh tĩnh.
3. **Viết `ios_steps`.** Đây là thứ duy nhất người chụp cột iOS đọc được. Nó phải mô tả **cùng một
   màn biểu diễn** mà cột Android đang chứng minh, không phải một thao tác na ná.
4. **Chạy thử và xem lại kết quả bằng mắt.** Đừng tin là nó đúng chỉ vì lệnh chạy xong không lỗi —
   nhắm sai vẫn cho ra một tấm ảnh hợp lệ của màn hình sai, và stream hỏng vẫn cho ra một GIF hợp
   lệ toàn nhiễu.
5. **Nhúng vào [`README.md`](README.md)** dưới dạng **bảng hai cột** như mọi mục khác, ô iOS là
   placeholder ghi rõ tên file đang chờ. Caption *in nghiêng* nói điều mà ảnh **chứng minh**, không
   phải mô tả lại thứ nhìn thấy được. Rồi **cập nhật mục 8 và 9 của file này**.

Song song đó, [`AGENTS.md`](AGENTS.md) vẫn yêu cầu mỗi màn mới có một `spec/<màn>.md`, và mọi thay
đổi Android quan sát được phải có handoff trong `docs/updates/YYYY-MM-DD.md` trước khi merge.

---

## 11. Checklist trước khi commit ảnh

- [ ] Đã **mở từng ảnh/GIF mới ra xem**. Nhắm sai vẫn tạo file thành công.
- [ ] Video trong khung player **không phải dải nhiễu màu** — xem mục 5.
- [ ] Không có ảnh nào bị bắt **giữa animation** — panel phải đứng yên hẳn.
- [ ] Không có ảnh nào còn **buffering** hoặc đang ở frame đen đầu stream.
- [ ] Không có thẻ *"Viewing full screen"* của hệ thống trong ảnh story/fullscreen.
- [ ] GIF **dưới ~900 KB** — công cụ tự kiểm tra và báo khi vượt, đồng thời giữ lại `.mov` để mã
      hoá lại bằng `--from`. Quá thì hạ `ios_gif_fps` / `ios_gif_width` / `ios_gif_colors` của
      capture đó, **không** đụng vào `gif_*` (đó là cột Android).
      GIF iOS nặng hơn GIF Android cùng cảnh: emulator Android rớt frame nên nhiều frame trùng nhau
      và nén gần như miễn phí, còn simulator quay đủ 60 fps toàn màn hình nên hầu như không frame
      nào giống frame nào. Đo được: **một frame có chuyển động tốn ~30 KB ở bề ngang 200 px**, nên
      ngân sách thực tế là khoảng **30 frame có chuyển động**. Vì vậy cảnh cuộn dài nên dùng **ít
      cú vuốt nhưng vuốt xa hơn** thay vì nhiều cú vuốt ngắn — cùng đi qua chừng ấy section.
- [ ] `python3 tools/capture_media.py list` khớp với bảng ở mục 8.
- [ ] Mọi đường dẫn ảnh trong README đều tồn tại, và mọi file trong `docs/images/` đều được dùng:

```bash
python3 - <<'PY'
import re, io, os
s = io.open('README.md', encoding='utf-8').read()
refs = set(re.findall(r'!\[[^\]]*\]\(([^)]+)\)', s)) | set(re.findall(r'<img src="([^"]+)"', s))
print("THIEU FILE:", [r for r in sorted(refs) if not os.path.exists(r)] or "khong")
print("ANH THUA:", sorted({f for f in os.listdir('docs/images')} - {os.path.basename(r) for r in refs}) or "khong")
PY
```

---

## 12. Những lỗi đã gặp, đừng gặp lại

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Khung video là **dải nhiễu xanh/hồng**, mọi thứ khác bình thường | Decoder emulator hỏng với H.264 Main/High | Mục 5 — dùng fixture `APPLE_TS` |
| Player hiện **"This video is not available for your account"** | `SINTEL` trả 403; app render đúng lỗi not-entitled | Đổi item, đừng đổi app |
| Ảnh có thẻ trắng **"Viewing full screen"** | Android nhắc immersive lần đầu | `settings put secure immersive_mode_confirmations confirmed` |
| Chụp ra **splash screen** | Gửi cử chỉ khi app còn đang verify class (debug build khởi động rất chậm) | Đã sửa: `relaunch()` chờ node `Videos for you`, không chờ theo đồng hồ |
| Ảnh "Settings" nhưng controller đang ẩn | Chạm toggle đúng lúc controller đang hiện | Mục 6 — dùng `SHOW_CONTROLLER` |
| `uiautomator dump` trả về rỗng / "could not get idle state" | Video đang render, window không bao giờ idle | Trong player dùng toạ độ phân số, đừng dùng `desc` |
| Kéo player xuống không thu nhỏ | Cử chỉ bắt đầu trên một control, hoặc dùng `swipe` thay vì `drag` | `PLAYER_EMPTY_SURFACE` + `drag:…,1100` |
| Chạm vào card mà mở **nhầm item** | Nhắm trúng mẩu card ló ra ở mép carousel | Đã sửa: `find_node` lấy node rộng nhất, `min_width=200` |
| `scrollto` báo không tìm thấy card | Card đó nằm giữa rail, chưa bao giờ hiện ra theo chiều ngang | Chọn item ở **đầu** một rail, hoặc thêm bước vuốt ngang |
| ANR ngay khi mở app | Máy đang build iOS/Kotlin Native song song, emulator bị đói CPU | Đừng chụp trong lúc build; chạy tuần tự |
| GIF quá nặng (>1 MB) | `duration` dài, hoặc nền là video đang chạy nên mọi frame đều khác nhau | Giảm `gif_width` / `gif_fps`, rút ngắn `steps` |
| Emulator không tải được ảnh dù ping được | Còn kẹt throttle từ lần thử `adb emu network speed` | `adb emu network speed full` |
| GIF iOS ra **4 MB** dù cảnh chẳng có gì | Nền trong suốt chiếm mất ô bảng màu mà GIF dùng để chỉ ghi phần thay đổi | Đã sửa: nền thân máy là màu đặc — xem mục 1 |
| Short iOS **thừa một dải hình lạ ở đáy** | `ScrollView` phân trang luôn tràn qua safe area rồi bù bằng content inset, nên cửa sổ nhìn thấy cao hơn `geometry.size`; trang cao bằng `geometry.size` để lộ đúng phần chênh đó của **trang kế tiếp** | Đã sửa: trang cao bằng cửa sổ (`+ safeAreaInsets`), `ScrollView` `.ignoresSafeArea()`, chrome tự gánh lại inset |
| Short đầu tiên chỉ hiện thumbnail, không phát | `ScrollView` tràn safe area chốt vị trí chậm hơn một nhịp nên `scrollPosition` chưa báo trang đầu, không trang nào `isActive` | Đã sửa: `onAppear` tự nhận selection đang chia sẻ |
