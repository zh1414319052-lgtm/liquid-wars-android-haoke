# 失败与修正（保留）

- 网站图片准备接口本次返回 local，初稿只处理 direct，断言退出；补上实时契约允许的 local 原始图片上传。首次原始上传 HTTP403（未捕获具体响应体，原因未确认）；按既有读取器补齐 Haoke-Agent/Accept 请求头后，三张图上传成功。没有改图片存储配置，没有绕过证书或使用其他凭据，也未因图片失败重发软件版本。

- v6 复核发现传送试放读取了不可见敌人的占位，可能泄露隐藏情报。v7 只以已观察占位评估落点，未知占位交给原有合法指令判定；新增“隐藏敌人移动不改变传送方案”定向检查。v6 192例结果保留，但不作为最终发布身份。

- v5 录屏盲看发现 map1/seed17901 白队长留右边缘。edge-before.csv 证明规划目标 x=786 超出可执行触点 x<784 的范围，反复追逐无效终点。v6 将导航节点与执行层的四边控制范围统一。edge-after-v6-correct.csv 同条件在 5.95 秒已到 x=528，随后参战、撤退和抢道具。v5 的 final-matrix 命名只代表当时候选，不是最终验收；其签名候选 liquidwars-1.79-final.apk 不发布。
- v6 第一次 trace/unit 命令漏传 assets 根参数，进程退出 1，无有效结果；空 edge-after-v6.csv 保留。正确参数运行见 edge-after-v6-correct.csv，定向核心检查 1892 项通过。

- 基线设备脚本首次调用在安装前报 TypeError: 'int' object is not callable。局部地图变量map遮蔽Python内建map；改map_id。没有开始安装或游戏运行，没有API请求。随后重新执行同一基线流程。
- 核心定向夹具首次失败 fixture placement：错误假设fallback blank-map完全无障碍。修正为仅在测试State中调用现有excavate构造明确的开放测试场；正式地图/规则不改。这些是合成场景，不能当作自然开局或真实人类试玩。
- pilot-v1小样本中的map1/54失败显示进退反复；增加840ms行动切换迟滞、撤退短保持以及受阻时靠近的中间路点。v1结果全部保留，v2同32例单挑14/16、六队14/16；仍是调参样本，不能当作未见地图最终胜率。

## Android build and performance findings
- android-v1 failed Clang -Werror: existing minimap indentation exposed by new include plus two new landing-function warnings. Corrected formatting/unused variables, retained log. android-v2 and v3 built successfully.
- after-v2, map54, 4500/team: 22.395 s wall advanced only 18.956 s simulation; baseline 22.396 s advanced 22.134 s. This regression blocks release. v3 caches pickup edge checks and uses bounded-weight Dial search; retest pending.
- QA isolation correction: baseline and after-v2 debug startup used existing ModelGameSupport which loaded saved model configurations in memory before masking all model teams off. No configuration was extracted or modified by the agent, and no provider requests were made. Earlier test JSON config_read=false/binding read_api_config=false describes agent access only and must NOT be interpreted as no in-app load. New debug qa_model_teams=0 bypasses ModelStore entirely. Release settings behavior unchanged.
- full-v2 and pilot-v1/v2 use a fixed execution-step cap; a rewind can reset game elapsed time. Their new_win field is capped-run score leadership, not necessarily a completed timed-match win. Keep these diagnostics; final benchmark will use the actual match clock and explicit termination.

- First package-check adapter incorrectly assumed four .so files instead of four ABIs. Actual baseline/current both contain12 libraries (nativeinterface, ODE, libc++ per ABI). Corrected assertion to the baseline library path set; also verify unchanged ODE/libc++ hashes and ELF64 alignment. This was a check-script error, not an APK omission.
