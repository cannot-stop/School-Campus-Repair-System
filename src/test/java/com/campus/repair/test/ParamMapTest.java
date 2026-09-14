package com.campus.repair.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Map;

import com.campus.repair.web.ParamMap;

/** 参数解析自测：验证 JSON / 表单请求体解析与内嵌服务器读取逻辑一致 */
public class ParamMapTest {

    public static void main(String[] args) throws Exception {
        Charset utf8 = Charset.forName("UTF-8");
        int failed = 0;

        // 1. 内嵌服务器读取逻辑等价性
        String json = "{\"username\":\"student\",\"password\":\"123456\"}";
        String read = ParamMap.readBody(new ByteArrayInputStream(json.getBytes(utf8)), 1024 * 1024);
        System.out.println("readBody = " + read);
        if (!json.equals(read)) {
            System.out.println("  [FAIL] readBody 内容不一致");
            failed++;
        } else {
            System.out.println("  [OK] readBody 读取完整");
        }

        // 2. JSON 解析
        Map<String, String> parsed = ParamMap.parseJson(read);
        System.out.println("parseJson = " + parsed);
        if (!"student".equals(parsed.get("username")) || !"123456".equals(parsed.get("password"))) {
            System.out.println("  [FAIL] JSON 参数解析失败");
            failed++;
        } else {
            System.out.println("  [OK] JSON 参数解析正确");
        }

        // 3. 表单解析
        Map<String, String> form = ParamMap.parseUrlEncoded("username=student&password=123456&role=reporter");
        if (!"student".equals(form.get("username")) || !"reporter".equals(form.get("role"))) {
            System.out.println("  [FAIL] 表单参数解析失败：" + form);
            failed++;
        } else {
            System.out.println("  [OK] 表单参数解析正确");
        }

        // 4. 中文与 URL 编码
        Map<String, String> cn = ParamMap.parseJson("{\"building\":\"实验楼\",\"room\":\"215\"}");
        if (!"实验楼".equals(cn.get("building"))) {
            System.out.println("  [FAIL] 中文参数解析失败：" + cn);
            failed++;
        } else {
            System.out.println("  [OK] 中文参数解析正确（" + cn.get("building") + "）");
        }

        // 5. 数组解析（耗材明细）
        java.util.List<Map<String, String>> items = ParamMap.parseJsonArray(
                "[{\"matId\":1,\"useCount\":2},{\"matId\":3,\"useCount\":1}]");
        if (items.size() != 2 || !"1".equals(items.get(0).get("matId")) || !"1".equals(items.get(1).get("useCount"))) {
            System.out.println("  [FAIL] 数组解析失败：" + items);
            failed++;
        } else {
            System.out.println("  [OK] 数组解析正确（" + items.size() + " 项）");
        }

        System.out.println(failed == 0 ? "参数解析自测全部通过 -> PASS" : ("参数解析自测失败 " + failed + " 项"));
        System.exit(failed == 0 ? 0 : 1);
    }
}
