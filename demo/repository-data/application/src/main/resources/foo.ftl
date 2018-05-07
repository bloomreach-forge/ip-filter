
<#include "../include/imports.ftl">
<#--  * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.-->


<#-- @ftlvariable name="item" type="org.example.beans.EventsDocument" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>
  <div>
    <#list pageable.items as item>
        <@hst.link var="link" hippobean=item />
      <article class="has-edit-button">
        <@hst.cmseditlink hippobean=item/>
          <h3><a href="${link}">${item.title?html}</a></h3>
        <#if item.date?? && item.date.time??>
          <p><@fmt.formatDate value=item.date.time type="both" dateStyle="medium" timeStyle="short"/></p>
        </#if>
        <#if item.enddate?? && item.endDate.time??>
          <p><@fmt.formatDate value=item.endDate.time type="both" dateStyle="medium" timeStyle="short"/></p>
        </#if>
          <p>${item.location?html}</p>
          <p>${item.introduction?html}</p>
      </article>
    </#list>
    <#if cparam.showPagination>
        <#include "../include/pagination.ftl">
    </#if>
  </div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <div>
      <img src="<@hst.link path='/images/essentials/catalog-component-icons/events-list.png'/>"> Click to edit Event List
  </div>
</#if>

