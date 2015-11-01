class PostsController < ApplicationController

  def index
    @posts = Post.where(params.permit(:author_id, :likes))
    render json: @posts
  end

  def show
    render json: Post.find(params[:id])
  end

  def create
    @post = Post.create! params.require(:post).permit(:author_id, :title, :body, :likes)
    render json: @post
  end

  def update
    @post = Post.find params[:id]
    if @post.nil?
      record_not_found
    else
      @post.update! params.require(:post).permit(:author_id, :title, :body, :likes)
      render json: @post
    end
  end

  def destroy
    render json: Post.find(params[:id]).destroy
  end

end
