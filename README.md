# hessian-scanner
用法：
    接口：@HessianCustomer(provider = "提供者标识", ref = "bean引用名", value = "hessian暴露的服务名")
    实现类：@HessianProvider

    消费者：	
        <bean class="HessianCustomerScanner">
            <property name="basePackage" value="com.xxx"/> // 包扫描路径
            <property name="urls">  // 提供者配置
                <map>
                    <entry key="db">    // 提供者标识
                        <value>${hessian.dbServiceUrl}</value> // 提供者URL
                    </entry>
                </map>
            </property>
        </bean>
    提供者：<bean class="HessianProviderScanner" />