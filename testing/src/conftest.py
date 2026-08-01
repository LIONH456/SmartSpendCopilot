import os, sys
from pathlib import Path
import time

import pytest
from appium import webdriver

# 把 src Folder的路径，强行插入到 Python “地址簿”（sys.path）的第 0 位（最前面）
sys.path.insert(0, str(Path(__file__).resolve().parent))

# scope="session" 意思是：整个测试期间，手机 App 只启动一次，不需要跑完一个用例就重启一次，省时间！
@pytest.fixture(scope="session")
def driver():
    # 告诉 Appium 自动化服务器，我们要控制的是哪台手机、哪个 App
    capabilities = {
        "platformName": "Android",            # 🤖 目标系统是安卓
        "automationName": "UiAutomator2",     # ⚙️ 驱动引擎，安卓自动化认准这个
        "deviceName": "Android Emulator",     # 📱 设备名字，其实写啥都行，主要是给电脑看的
        "appPackage": "com.example.smartspend_mobile",   # 📦 咱们记账 App 的“身份证号”（包名） # adb shell dumpsys window | findstr mCurrentFocus
        "appActivity": ".MainActivity",       # 🎬 App 启动时的第一个“大门”（启动Activity）
        "noReset": True,                      # 🧹 别每次都清除缓存！保留登录状态，方便后面测试
        "ensureWebviewsHavePages": True
    }

    #  🌐 Appium Server 默认开在本地的 4723 端口，我们把参数打包发过去，让它连手机
    appium_server_url = "http://localhost:4723"

    print("\n[INFO] Establishing connection to the Appium server and launching the application...")

    # 🎬 正式建立连接，这行代码执行完，手机屏幕就会亮起，App 就会被拉起来！
    driver = webdriver.Remote(appium_server_url, capabilities)

    # 🎁 把这个活生生的 driver 实例“借给”测试用例去用
    yield driver

    # 🧹 【Teardown环节】当所有用例全部跑完后，pytest 会自动回到这里执行下面这一行
    print("\n[INFO] Test session execution completed. Terminating the WebDriver session and releasing device resources...")
    driver.quit()


import pytest


# 🛠️ 这是一个 pytest 的内置钩子（Hook）函数，用来监听每个测试用例的运行状态
# 每个Test Case跑完都会执行一次
@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    # 🛑 先让用例正常去跑，拿到跑完之后的报告结果
    outcome = yield
    report = outcome.get_result()

    # 🔍 我们只在“测试步骤执行阶段（call）”且“结果为失败（failed）”时才出手
    if report.when == "call" and report.failed:
        # 🔌 从测试用例中把那个活生生的手机驱动（driver）实例揪出来
        driver = item.funcargs.get("driver")

        if driver:
            print(f"\n[ERROR] Failure detected in test case '{item.name}'. Initializing automatic device screenshot...")
            # 📁 规定好截图保存的路径和文件名，用用例名字命名，方便找
            screenshot_dir = "screenshots"
            if not os.path.exists(screenshot_dir):
                os.makedirs(screenshot_dir)

            timestamp = time.strftime("%Y%m%d-%H%M%S")

            screenshot_path = os.path.join(screenshot_dir, f"{item.name}_{timestamp}.png")

            # 📷 真正命令手机截图并保存到本地
            driver.save_screenshot(screenshot_path)
            print(f"[INFO] Screenshot successfully captured and saved to: {screenshot_path}")


# ==============================================================================
# 💡 老油条留给未来的你的【大白话说明书】 💡
# ==============================================================================
# 这个文件是 pytest 框架的“大管家”，不需要手动 import 它，运行测试时它会自动生效。
#
# 🛠️ 步骤一：环境准备与“寻路魔法” (sys.path.insert)
# ------------------------------------------------------------------------------
# 【干嘛的】: 把我们项目的根目录（src）强行塞进 Python 的“内部地址簿”里。
# 【为什么】: 电脑很笨，如果不塞进去，当你在别的地方写 `from pages.xxx import xxx` 时，
#            Python 就会迷路报错（ModuleNotFoundError）。这一行保证了全项目引路畅通无阻。
#
# 🤖 步骤二：手机连接与启动配置 (def driver())
# ------------------------------------------------------------------------------
# 【干嘛的】: 1. 定义了一个叫 `driver` 的夹具（Fixture）。
#            2. 里面打包了一堆参数（capabilities），告诉 Appium 我们要控哪台手机、哪个 App。
#            3. 通过 `webdriver.Remote` 真正把手机里的 SmartSpend App 顶起来。
# 【为什么】:
#   - `scope="session"`: 整个测试期间 App 只启动一次，不频繁重启，省下大量时间。
#   - `noReset: True`: 保持登录状态，别每次运行都像刚下载 App 一样清空数据。
#   - `yield driver`: 关键魔术！`yield` 之前是“前置准备”（开机），把 driver 借给用例用；
#     用例全部跑完后，会自动回来执行 `yield` 之后的 `driver.quit()`，也就是“后置清理”（关机擦屁股）。
#
# 📸 步骤三：失败自动抓拍截图 (pytest_runtest_makereport)
# ------------------------------------------------------------------------------
# 【干嘛的】: 这是一个全局的“特务监听器”，时刻盯着每一个测试用例的死活。
# 【为什么】: 自动化测试经常在后台或者夜里跑，如果报错了我们不知道当时手机界面长啥样。
#            这个监听器一旦发现用例“失败了（failed）”，就会立刻启动手机截图功能，
#            把当时惨烈的故障现场拍下来，加上时间戳，存进 `screenshots` 文件夹，方便我们捉虫。
# ==============================================================================
