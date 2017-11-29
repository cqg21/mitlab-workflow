drop table if exists t_step_user_group;
drop table if exists t_step_args;
drop table if exists t_history_step;
drop table if exists t_current_step;
drop table if exists t_workflow;

drop table if exists t_global_permission;

drop table if exists t_system_sequence;

CREATE TABLE t_system_sequence (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  name varchar(64) DEFAULT NULL,
  value bigint(20) DEFAULT NULL,
  gmt_modified timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY id (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

create table t_global_permission (
	name varchar(200),
	url varchar(200) not null,
	required boolean not null,
	decorator varchar(64),
	access_check boolean not null,
	context_path varchar(64) not null,
	authority varchar(64) not null,
	primary key(context_path,url),
	unique(authority)
);

create table t_workflow(
	workflow_id bigint not null auto_increment primary key,
	workflow_name varchar(64) not null,
	workflow_status varchar(10) not null,
	workflow_phase varchar(10) not null,
	crt_time datetime not null default current_timestamp,
	main_flow_id bigint,
	main_flow_join_step_id bigint
) engine=innodb default charset=utf8;

create table t_current_step(
	id bigint not null auto_increment primary key,
	step_id varchar(32) not null,
	step_name varchar(64),
	user_group varchar(128),
	caller varchar(32),
	start_date datetime,
	due_date datetime,
	finish_date datetime,
	status varchar(32),
	workflow_id bigint not null,
	action_id varchar(32),
	prev_id bigint,
	index(workflow_id),
	index(prev_id)
) engine=innodb default charset=utf8;
create table t_history_step(
	id bigint not null auto_increment primary key,
	step_id varchar(32) not null,
	step_name varchar(64),
	user_group varchar(128),
	caller varchar(32),
	start_date datetime,
	due_date datetime,
	finish_date datetime,
	status varchar(32),
	workflow_id bigint not null,
	action_id varchar(32),
	prev_id bigint,
	index(workflow_id),
	index(prev_id)
) engine=innodb default charset=utf8;

create table t_step_user_group(
	step_id bigint not null,
	refer_user varchar(32) not null,
	refer_user_group varchar(32) not null,
	crt_time datetime not null default current_timestamp,
	workflow_id bigint not null,
	primary key(step_id,refer_user,refer_user_group),
	index(workflow_id)
) engine=innodb default charset=utf8;

create table t_step_args(
	workflow_id bigint not null,
	step_id bigint not null,
	step_args longblob,
	args_type int comment '0:current_step_args;1:history_step_args',
	primary key(workflow_id,step_id,args_type)
) engine=innodb default charset=utf8;