class PostsController < ApplicationController

  def index
    @posts = Post.where(params.permit(:author_id, :likes))
    render json: @posts
  end

  def show
    render json: Post.find(params[:id])
  end

  def create
    @post = Post.create params.permit(:author_id, :title, :body, :likes)
    render json: @post
  end

  def update
    @post = Post.find params[:id]
    @post.update(params.permit(:author_id, :title, :body, :likes)) unless @post.nil?
    render json: @post
  end

  def destroy
    render json: Post.find(params[:id]).destroy
  end

end
