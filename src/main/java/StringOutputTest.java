import org.apache.storm.shade.org.apache.commons.lang.StringEscapeUtils;


public class StringOutputTest {

	public static void main(String[] args) {
		String test = "application\"ucenter-dubbo-2.3\u0014createTime&2017-04-17 09:10:15\nlevel\nDEBUG\u000EappName\"ucenter-dubbo-2.3\floggerXdao.UcCompanyCompanyMapper.getCompanyCompany\bhost\u0018192.168.1.51\ntopic\"ucenter-dubbo-2.3 createTimeInLong\u001A1492391415452\fthread`DubboServerHandler-192.168.1.51:20882-thread-192\fbranch\fmaster\u0000.==> Parameters: 1(Long)	354";
        System.out.println(StringEscapeUtils.escapeCsv(test));    
	}

}
