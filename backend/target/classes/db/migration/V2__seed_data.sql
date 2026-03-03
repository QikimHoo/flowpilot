INSERT INTO users (username, display_name, dept, roles) VALUES
('u_leader', '行领导', '管理层', '["ROLE_LEADER"]'::jsonb),
('u_pmo', 'PMO管理员', '信息技术部', '["ROLE_PMO"]'::jsonb),
('u_pm', '项目经理李四', '信贷研发组', '["ROLE_PM"]'::jsonb),
('u_owner', '模块负责人王五', '信贷研发组', '["ROLE_MODULE_OWNER"]'::jsonb),
('u_compliance', '合规联络人赵六', '内控合规部', '["ROLE_COMPLIANCE"]'::jsonb),
('u_viewer', '只读观察员', '业务条线', '["ROLE_VIEWER"]'::jsonb);

INSERT INTO nodes (id, node_type, node_name, owner_user_id, owner_dept, parent_id, path, sort_order, as_of_date, weights, plan, actual, computed)
VALUES
('PJT-2026-0001', 'project', '信贷域-贷款审批优化', 'u_pm', '信贷研发组', NULL, '/PJT-2026-0001', 0, '2026-02-25',
 '{"req":0.15,"dev":0.35,"test":0.25,"milestone":0.15,"compliance":0.10}'::jsonb,
 '{}'::jsonb,
 '{}'::jsonb,
 '{"planScore":0.55,"actualScore":0.42,"deviation":0.236,"trafficLight":"RED","deviationTopFactors":[{"dim":"test","contrib":0.07},{"dim":"dev","contrib":0.05},{"dim":"compliance","contrib":0.02}],"complianceKeyOverdue":false}'::jsonb),

('L1-001', 'l1', '业务处理', 'u_pm', '信贷研发组', 'PJT-2026-0001', '/PJT-2026-0001/L1-001', 0, '2026-02-25',
 '{"req":0.15,"dev":0.35,"test":0.25,"milestone":0.15,"compliance":0.10}'::jsonb,
 '{}'::jsonb,
 '{}'::jsonb,
 '{"planScore":0.55,"actualScore":0.42,"deviation":0.236,"trafficLight":"RED","deviationTopFactors":[{"dim":"test","contrib":0.07},{"dim":"dev","contrib":0.05},{"dim":"compliance","contrib":0.02}],"complianceKeyOverdue":false}'::jsonb),

('L2-001', 'l2', '贷款审批', 'u_owner', '信贷研发组', 'L1-001', '/PJT-2026-0001/L1-001/L2-001', 0, '2026-02-25',
 '{"req":0.15,"dev":0.35,"test":0.25,"milestone":0.15,"compliance":0.10}'::jsonb,
 '{}'::jsonb,
 '{}'::jsonb,
 '{"planScore":0.55,"actualScore":0.42,"deviation":0.236,"trafficLight":"RED","deviationTopFactors":[{"dim":"test","contrib":0.07},{"dim":"dev","contrib":0.05},{"dim":"compliance","contrib":0.02}],"complianceKeyOverdue":false}'::jsonb),

('REQ-001', 'req', '规则引擎改造', 'u_owner', '信贷研发组', 'L2-001', '/PJT-2026-0001/L1-001/L2-001/REQ-001', 0, '2026-02-25',
 '{"req":0.15,"dev":0.35,"test":0.25,"milestone":0.15,"compliance":0.10}'::jsonb,
 '{"kpi":{"req":{"expectedDone":1.0,"confirmedCount":18,"baselineCount":20},"dev":{"expectedDone":1.0,"plannedTasks":40},"test":{"expectedDone":0.5,"plannedCases":200},"milestone":{"expectedDone":0.0,"plannedMilestones":3},"compliance":{"expectedDone":0.0,"requiredDocs":8}}}'::jsonb,
 '{"kpi":{"req":{"done":1.0,"changeRequested":6,"changeAnalyzed":4,"changeImplemented":2},"dev":{"done":0.9,"doneTasks":36,"blockedTasks":2,"reworkTasks":1},"test":{"done":0.3,"executedCases":60,"defectClosedRate":0.4},"milestone":{"done":0.0,"onTimeRate":0.0,"delayDays":0},"compliance":{"done":0.0,"archivedDocs":0,"overdueDocs":0}}}'::jsonb,
 '{"planScore":0.625,"actualScore":0.54,"deviation":0.136,"trafficLight":"YELLOW","deviationTopFactors":[{"dim":"test","contrib":0.05},{"dim":"dev","contrib":0.035},{"dim":"compliance","contrib":0.0}],"complianceKeyOverdue":false}'::jsonb),

('REQ-002', 'req', '审批流改造', 'u_owner', '信贷研发组', 'L2-001', '/PJT-2026-0001/L1-001/L2-001/REQ-002', 1, '2026-02-25',
 '{"req":0.15,"dev":0.35,"test":0.25,"milestone":0.15,"compliance":0.10}'::jsonb,
 '{"kpi":{"req":{"expectedDone":0.6},"dev":{"expectedDone":0.6},"test":{"expectedDone":0.4},"milestone":{"expectedDone":0.2},"compliance":{"expectedDone":0.3}}}'::jsonb,
 '{"kpi":{"req":{"done":0.6},"dev":{"done":0.6},"test":{"done":0.4},"milestone":{"done":0.2},"compliance":{"done":0.3}}}'::jsonb,
 '{"planScore":0.5,"actualScore":0.5,"deviation":0.0,"trafficLight":"RED","deviationTopFactors":[{"dim":"dev","contrib":0.0},{"dim":"test","contrib":0.0},{"dim":"req","contrib":0.0}],"complianceKeyOverdue":true}'::jsonb);

INSERT INTO compliance_items (node_id, doc_type, is_key, required_at, submitted_at, status, overdue_days, attachment_url, meta)
VALUES
('REQ-002', '需求评审记录', true, '2026-02-20', NULL, 'OVERDUE', 5, 'https://example.com/docs/req-review.pdf', '{"owner":"u_compliance"}'::jsonb),
('REQ-001', '测试报告归档', false, '2026-02-28', '2026-02-28', 'SUBMITTED', 0, 'https://example.com/docs/test-report.pdf', '{"owner":"u_owner"}'::jsonb);

INSERT INTO warnings (node_id, level, status, deviation, reason, deviation_top_factors, sla_due_at, triggered_at, assignees, action_log)
VALUES
('REQ-001', 'YELLOW', 'OPEN', 0.136, '计划与实际偏差触发',
 '[{"dim":"test","contrib":0.05},{"dim":"dev","contrib":0.035},{"dim":"compliance","contrib":0.0}]'::jsonb,
 NOW() + INTERVAL '2 day', NOW() - INTERVAL '7 day',
 '["u_owner"]'::jsonb,
 '[{"at":"2026-02-24T09:00:00","action":"seed_warning"}]'::jsonb);

INSERT INTO user_node_acl (user_id, node_id, permission) VALUES
('u_pm', 'PJT-2026-0001', 'ADMIN'),
('u_owner', 'L2-001', 'WRITE'),
('u_compliance', 'PJT-2026-0001', 'READ'),
('u_viewer', 'REQ-001', 'READ');
