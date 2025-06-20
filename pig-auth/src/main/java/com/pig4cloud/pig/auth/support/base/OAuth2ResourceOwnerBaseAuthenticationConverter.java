package com.pig4cloud.pig.auth.support.base;

import com.pig4cloud.pig.common.security.util.OAuth2EndpointUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jumuning
 * @date 2022-06-02
 *
 * 自定义模式认证转换器
 */
//o2 2. 配置converter 这里定义了一个抽象类，实现了接口AuthenticationConverter的convert方法，这个方法调用了support，来交给容器来判断使用哪个
// 写了个checkParams方法，来获取请求里传的参数
// 写了个 buildToken来生成自定义的token
public abstract class OAuth2ResourceOwnerBaseAuthenticationConverter<T extends OAuth2ResourceOwnerBaseAuthenticationToken>
		implements AuthenticationConverter {

	/**
	 * 是否支持此convert
	 * @param grantType 授权类型
	 * @return
	 */
	public abstract boolean support(String grantType);

	/**
	 * 校验参数
	 * @param request 请求
	 */
	public void checkParams(HttpServletRequest request) {

	}

	/**
	 * 构建具体类型的token
	 * @param clientPrincipal
	 * @param requestedScopes
	 * @param additionalParameters
	 * @return
	 */
	public abstract T buildToken(Authentication clientPrincipal, Set<String> requestedScopes,
			Map<String, Object> additionalParameters);

	@Override
	public Authentication convert(HttpServletRequest request) {

		//o2 2.1 获取前端传的grant_type，来判别用哪个授权converter

		// grant_type (REQUIRED)
		String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
		if (!support(grantType)) {
			return null;
		}

		//o2 2.2 获取request传的所有参数，放在MultiValueMap里，这个map相当于Map<String, String []> ，但是更简便写
		MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);
		// scope (OPTIONAL)
		//o2 ? 2.3 获取scope，到目前位置，仍不知道scope干什么用
		String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
		// scope不是必填的，但是一旦前端传了这个参数，就只能有一个，不能?scope=read&scope=write
		if (StringUtils.hasText(scope) && parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
			OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.SCOPE,
					OAuth2EndpointUtils.ACCESS_TOKEN_REQUEST_ERROR_URI);
		}

		//o2 2.4 将前端传的scope放到Set里，比如将scope=read write，转换为有read和write的Set
		Set<String> requestedScopes = null;
		if (StringUtils.hasText(scope)) {
			requestedScopes = new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
		}

		//o2 2.5 调用校验前端参数的方法，因为converter的目的就是将request传的有效参数，转化为token，所以要进行校验
		// 如果是手机号converter，那么就只获取手机号
		// 如果是用户密码，那么就获取用户和密码，来生成对应的token
		// 校验个性化参数
		checkParams(request);


		//o2 这里的客户端Authentication已经认证过了，可能是true也可能是false，这里不根据认证的状态来判断是否继续往下走
		// 因为converter的职责只是提取参数，不做认证后的逻辑业务，这个交给后面的provider进行处理
		// ClientSecretAuthenticationProvider  这个是客户端认证器
//		请求 → TokenEndpointFilter → Converter（封装请求参数） → AuthenticationManager.authenticate()
//                                                           ↓
//		Provider.supports() 判断 → Provider.authenticate()
//                                                                 ↓
//		getAuthenticatedClientElseThrowInvalidClient() 安全检查
		Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
		if (clientPrincipal == null) {
			OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ErrorCodes.INVALID_CLIENT,
					OAuth2EndpointUtils.ACCESS_TOKEN_REQUEST_ERROR_URI);
		}

		//o2 2.6 除了grant_type 和 scope的其他参数，都作为额外的参数，去生成自定义token
		//o2? 这里为什么不排除client_id client_secret

		// 扩展信息
		Map<String, Object> additionalParameters = parameters.entrySet()
			.stream()
			.filter(e -> !e.getKey().equals(OAuth2ParameterNames.GRANT_TYPE)
					&& !e.getKey().equals(OAuth2ParameterNames.SCOPE))
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));

		// 创建token
		//o2 这里对于单个元素，去重用的Set， k->v用的就是Map
		//o2 2.7 这里就是生成了自定义token，那么这个token该交由给provider来处理了
		return buildToken(clientPrincipal, requestedScopes, additionalParameters);

	}

}
