-- Database generated with pgModeler (PostgreSQL Database Modeler).
-- PostgreSQL version: 9.3
-- Project Site: pgmodeler.com.br
-- Model Author: ---

SET check_function_bodies = false;
-- ddl-end --

-- Database creation must be done outside an multicommand file.
-- These commands were put in this file only for convenience.
-- -- object: "fink-db" | type: DATABASE --
-- CREATE DATABASE "fink-db"
-- 	ENCODING = 'UTF8'
-- 	LC_COLLATE = 'C'
-- 	LC_CTYPE = 'C'
-- 	TABLESPACE = pg_default
-- 	OWNER = development
-- ;
-- -- ddl-end --
-- 

-- object: public.setting | type: TABLE --
CREATE TABLE public.setting(
	id bigserial NOT NULL,
	title text NOT NULL,
	description text NOT NULL,
	keywords text NOT NULL,
	frontend text NOT NULL,
	categories text NOT NULL,
	"uploadDirectory" text NOT NULL,
	CONSTRAINT setting_pkey PRIMARY KEY (id)

);
-- ddl-end --
-- object: setting_title_unq | type: INDEX --
CREATE UNIQUE INDEX setting_title_unq ON public.setting
	USING btree
	(
	  (lower(title))
	)	WITH (FILLFACTOR = 90);
-- ddl-end --

COMMENT ON INDEX setting_title_unq IS 'title must be case insensitive unique';
-- ddl-end --

ALTER TABLE public.setting OWNER TO development;
-- ddl-end --

-- object: public.pages_id_seq | type: SEQUENCE --
CREATE SEQUENCE public.pages_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;
ALTER SEQUENCE public.pages_id_seq OWNER TO development;
-- ddl-end --

-- object: public.page | type: TABLE --
CREATE TABLE public.page(
	id bigserial NOT NULL,
	date timestamp NOT NULL,
	title text NOT NULL,
	author text NOT NULL,
	shortlink text NOT NULL,
	text text NOT NULL,
	CONSTRAINT page_pkey PRIMARY KEY (id)

);
-- ddl-end --
-- object: page_title_unq | type: INDEX --
CREATE INDEX page_title_unq ON public.page
	USING btree
	(
	  (lower(title)) ASC NULLS LAST
	)	WITH (FILLFACTOR = 90);
-- ddl-end --


ALTER TABLE public.page OWNER TO development;
-- ddl-end --

-- object: public.posts_id_seq | type: SEQUENCE --
CREATE SEQUENCE public.posts_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;
ALTER SEQUENCE public.posts_id_seq OWNER TO development;
-- ddl-end --

-- object: public.post | type: TABLE --
CREATE TABLE public.post(
	id bigserial NOT NULL,
	date timestamp NOT NULL,
	title text NOT NULL,
	author text NOT NULL,
	shortlink text NOT NULL,
	text text NOT NULL,
	CONSTRAINT post_pkey PRIMARY KEY (id)

);
-- ddl-end --
-- object: post_title_unq | type: INDEX --
CREATE INDEX post_title_unq ON public.post
	USING btree
	(
	  (lower(title)) ASC NULLS LAST
	)	WITH (FILLFACTOR = 90);
-- ddl-end --


ALTER TABLE public.post OWNER TO development;
-- ddl-end --

-- object: public.tags_id_seq | type: SEQUENCE --
CREATE SEQUENCE public.tags_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;
ALTER SEQUENCE public.tags_id_seq OWNER TO development;
-- ddl-end --

-- object: public.mtag | type: TABLE --
CREATE TABLE public.mtag(
	id bigserial NOT NULL,
	name text NOT NULL,
	CONSTRAINT mtag_pkey PRIMARY KEY (id)

);
-- ddl-end --
-- object: mtag_name_uniq | type: INDEX --
CREATE UNIQUE INDEX mtag_name_uniq ON public.mtag
	USING btree
	(
	  (lower(name))
	)	WITH (FILLFACTOR = 90);
-- ddl-end --

COMMENT ON INDEX mtag_name_uniq IS 'Names must be case insensitive unique';
-- ddl-end --

ALTER TABLE public.mtag OWNER TO development;
-- ddl-end --

-- object: public.categories_id_seq | type: SEQUENCE --
CREATE SEQUENCE public.categories_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;
ALTER SEQUENCE public.categories_id_seq OWNER TO development;
-- ddl-end --

-- object: public.category | type: TABLE --
CREATE TABLE public.category(
	id bigserial NOT NULL,
	name text NOT NULL,
	CONSTRAINT categories_pkey PRIMARY KEY (id)

);
-- ddl-end --
-- object: category_name_unq | type: INDEX --
CREATE INDEX category_name_unq ON public.category
	USING btree
	(
	  (lower(name))
	)	WITH (FILLFACTOR = 90);
-- ddl-end --


ALTER TABLE public.category OWNER TO development;
-- ddl-end --

-- object: public.images_id_seq | type: SEQUENCE --
CREATE SEQUENCE public.images_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;
ALTER SEQUENCE public.images_id_seq OWNER TO development;
-- ddl-end --

-- object: public.image | type: TABLE --
CREATE TABLE public.image(
	id bigserial NOT NULL,
	date timestamp NOT NULL,
	title text NOT NULL,
	author text NOT NULL,
	hash text NOT NULL,
	"contentType" text NOT NULL,
	filename text NOT NULL,
	CONSTRAINT image_pkey PRIMARY KEY (id)

);
-- ddl-end --
-- object: image_title_unq | type: INDEX --
CREATE INDEX image_title_unq ON public.image
	USING btree
	(
	  (lower(title)) ASC NULLS LAST
	)	WITH (FILLFACTOR = 90);
-- ddl-end --


ALTER TABLE public.image OWNER TO development;
-- ddl-end --

-- object: public.post_mtag | type: TABLE --
CREATE TABLE public.post_mtag(
	id bigserial NOT NULL,
	post_id bigint NOT NULL,
	mtag_id bigint NOT NULL,
	CONSTRAINT post_mtag_pkey PRIMARY KEY (id),
	CONSTRAINT post_mtag_uniq UNIQUE (post_id,mtag_id)

);
-- ddl-end --
ALTER TABLE public.post_mtag OWNER TO development;
-- ddl-end --

-- object: public.galleries_id_seq | type: SEQUENCE --
CREATE SEQUENCE public.galleries_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;
ALTER SEQUENCE public.galleries_id_seq OWNER TO development;
-- ddl-end --

-- object: public.gallery | type: TABLE --
CREATE TABLE public.gallery(
	id bigserial NOT NULL,
	cover_id bigint,
	date timestamp NOT NULL,
	title text NOT NULL,
	author text NOT NULL,
	shortlink text NOT NULL,
	text text NOT NULL,
	CONSTRAINT gallery_pkey PRIMARY KEY (id)

);
-- ddl-end --
-- object: gallery_title_unq | type: INDEX --
CREATE INDEX gallery_title_unq ON public.gallery
	USING btree
	(
	  (lower(title)) ASC NULLS LAST
	)	WITH (FILLFACTOR = 90);
-- ddl-end --


ALTER TABLE public.gallery OWNER TO development;
-- ddl-end --

-- object: public.gallery_image | type: TABLE --
CREATE TABLE public.gallery_image(
	id bigserial NOT NULL,
	gallery_id bigint NOT NULL,
	image_id bigint NOT NULL,
	sort bigint NOT NULL,
	CONSTRAINT gallery_image_pk PRIMARY KEY (id),
	CONSTRAINT gallery_image_uniq UNIQUE (gallery_id,image_id)

);
-- ddl-end --
ALTER TABLE public.gallery_image OWNER TO development;
-- ddl-end --

-- object: public.gallery_mtag | type: TABLE --
CREATE TABLE public.gallery_mtag(
	id bigserial NOT NULL,
	gallery_id bigint NOT NULL,
	mtag_id bigint NOT NULL,
	CONSTRAINT gallery_mtag_pkey PRIMARY KEY (id),
	CONSTRAINT gallery_mtag_uniq UNIQUE (gallery_id,mtag_id)

);
-- ddl-end --
ALTER TABLE public.gallery_mtag OWNER TO development;
-- ddl-end --

-- object: public.page_mtag | type: TABLE --
CREATE TABLE public.page_mtag(
	id bigserial NOT NULL,
	page_id bigint NOT NULL,
	mtag_id bigint NOT NULL,
	CONSTRAINT page_mtag_pkey PRIMARY KEY (id),
	CONSTRAINT page_mtag_unq UNIQUE (page_id,mtag_id)

);
-- ddl-end --
COMMENT ON CONSTRAINT page_mtag_unq ON public.page_mtag IS 'enforce the constraint for the true key';
-- ddl-end --
ALTER TABLE public.page_mtag OWNER TO development;
-- ddl-end --

-- object: public.post_category | type: TABLE --
CREATE TABLE public.post_category(
	id bigserial,
	post_id bigint NOT NULL,
	category_id bigint NOT NULL,
	CONSTRAINT post_category_pk PRIMARY KEY (id),
	CONSTRAINT post_category_uniq UNIQUE (post_id,category_id)

);
-- ddl-end --
-- object: post_mtag_post_fk | type: CONSTRAINT --
ALTER TABLE public.post_mtag ADD CONSTRAINT post_mtag_post_fk FOREIGN KEY (post_id)
REFERENCES public.post (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION NOT DEFERRABLE;
-- ddl-end --


-- object: post_mtag_tag_fk | type: CONSTRAINT --
ALTER TABLE public.post_mtag ADD CONSTRAINT post_mtag_tag_fk FOREIGN KEY (mtag_id)
REFERENCES public.mtag (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION NOT DEFERRABLE;
-- ddl-end --


-- object: gallery_cover_id_fk | type: CONSTRAINT --
ALTER TABLE public.gallery ADD CONSTRAINT gallery_cover_id_fk FOREIGN KEY (id,cover_id)
REFERENCES public.gallery_image (gallery_id,image_id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
-- ddl-end --


-- object: gallery_image_gallery_fk | type: CONSTRAINT --
ALTER TABLE public.gallery_image ADD CONSTRAINT gallery_image_gallery_fk FOREIGN KEY (gallery_id)
REFERENCES public.gallery (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
-- ddl-end --


-- object: gallery_image_image_fk | type: CONSTRAINT --
ALTER TABLE public.gallery_image ADD CONSTRAINT gallery_image_image_fk FOREIGN KEY (image_id)
REFERENCES public.image (id) MATCH FULL
ON DELETE SET DEFAULT ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
-- ddl-end --


-- object: gallery_mtag_gallery_fk | type: CONSTRAINT --
ALTER TABLE public.gallery_mtag ADD CONSTRAINT gallery_mtag_gallery_fk FOREIGN KEY (gallery_id)
REFERENCES public.gallery (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
-- ddl-end --


-- object: gallery_mtag_tag_fk | type: CONSTRAINT --
ALTER TABLE public.gallery_mtag ADD CONSTRAINT gallery_mtag_tag_fk FOREIGN KEY (mtag_id)
REFERENCES public.mtag (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
-- ddl-end --


-- object: page_mtag_page_fk | type: CONSTRAINT --
ALTER TABLE public.page_mtag ADD CONSTRAINT page_mtag_page_fk FOREIGN KEY (page_id)
REFERENCES public.page (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION NOT DEFERRABLE;
-- ddl-end --


-- object: page_mtag_tag_fk | type: CONSTRAINT --
ALTER TABLE public.page_mtag ADD CONSTRAINT page_mtag_tag_fk FOREIGN KEY (mtag_id)
REFERENCES public.mtag (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION NOT DEFERRABLE;
-- ddl-end --


-- object: post_category_post_fk | type: CONSTRAINT --
ALTER TABLE public.post_category ADD CONSTRAINT post_category_post_fk FOREIGN KEY (post_id)
REFERENCES public.post (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION NOT DEFERRABLE;
-- ddl-end --


-- object: post_category_category_fk | type: CONSTRAINT --
ALTER TABLE public.post_category ADD CONSTRAINT post_category_category_fk FOREIGN KEY (category_id)
REFERENCES public.category (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION NOT DEFERRABLE;
-- ddl-end --



