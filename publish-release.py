import json
import os
import sys
import urllib.error
import urllib.request


REPO = "OliverArcher/FolioFrame"
APK = os.environ["APK_PATH"]
SHA = os.environ["APK_SHA"]
BYTES = os.environ["APK_BYTES"]
TOKEN = os.environ["GH_TOKEN"]

HEADERS = {
    "Authorization": "Bearer " + TOKEN,
    "User-Agent": "FolioFrame",
    "Accept": "application/vnd.github+json",
}

NOTES = f"""## 卷影 · FolioFrame 0.5.0

### 书籍搜索接入微信读书

- 新增微信读书来源，中文书籍覆盖优于此前的 Open Library / Google Books
- 需在设置页填入自己的微信读书 API Key，由 weread.qq.com/r/weread-skills 生成
- 该接口仅允许自身域名作为来源，浏览器跨域规则会拦截 WebView 请求；Android 版通过原生通道发起请求绕开该限制
- 移除 Open Library 与 Google Books 来源
- Bangumi 保留为第二来源

### WebDAV 同步

- 设置页新增手动推送与从云端拉取
- 复用现有 ZIP 备份格式整包传输，推送覆盖云端同名文件，不保留历史版本
- 拉取会覆盖云端同 ID 记录，未在云端的本地记录保留，操作前有确认提示
- 服务器地址与密码只保存在本机，不写入备份包
- Android 通过原生通道执行 PUT 与 GET，绕过 WebDAV 服务器普遍不返回的 CORS 头

### 封面处理

- 移除「缓存在线封面到本机」按钮
- 导出与推送时自动把在线封面打入备份包，无需手动操作

### 字体

- 更纱黑体由全量 TTF 改为完整 WOFF2 分发，各约 33MB 降至 19.5MB / 20.1MB
- 保留完整字符集，标题字形不会回退到系统字体
- APK 体积与启动时字体解析开销同步下降

### 界面

- 设置页移除冗余说明文字，输入框说明精简为标签
- 推送与拉取、导出与导入两组按钮改为同行双按钮
- 导出备份与推送云端统一使用强调色

### 安装说明

- 版本 `0.5.0` / `versionCode 9`，包名 `io.folioframe.juanying`
- 可直接覆盖安装，保留原有数据
- 从 0.4.x 升级后需在设置页填入微信读书 API Key 才能搜索书籍

### 校验

- 文件大小：{BYTES} bytes
- SHA256：`{SHA}`
"""


def api(path, data=None, method=None):
    url = "https://api.github.com" + path
    headers = dict(HEADERS)
    body = None
    if data is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(data).encode("utf-8")
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read())


def main():
    try:
        existing = api(f"/repos/{REPO}/releases/tags/v0.5.0")
        print("release already exists:", existing["html_url"])
        release_id = existing["id"]
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        created = api(
            f"/repos/{REPO}/releases",
            {"tag_name": "v0.5.0", "name": "卷影 · FolioFrame 0.5.0", "body": NOTES,
             "draft": False, "prerelease": False},
            method="POST",
        )
        print("release created:", created["html_url"])
        release_id = created["id"]

    for asset in api(f"/repos/{REPO}/releases/{release_id}/assets"):
        if asset["name"] == os.path.basename(APK):
            api(f"/repos/{REPO}/releases/assets/{asset['id']}", method="DELETE")
            print("removed previous asset")

    name = "FolioFrame-0.5.0-release.apk"
    upload_url = (
        f"https://uploads.github.com/repos/{REPO}/releases/{release_id}/assets"
        f"?name={name}"
    )
    with open(APK, "rb") as handle:
        payload = handle.read()
    request = urllib.request.Request(
        upload_url,
        data=payload,
        headers={
            "Authorization": "Bearer " + TOKEN,
            "User-Agent": "FolioFrame",
            "Content-Type": "application/vnd.android.package-archive",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=600) as response:
        asset = json.loads(response.read())
    print("asset uploaded:", asset["name"], asset["size"])
    print("download:", asset["browser_download_url"])


if __name__ == "__main__":
    main()
