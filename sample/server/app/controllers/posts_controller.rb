class PostsController < ApplicationController

  def index
    @posts = Post.where(params.permit(:author_id, :likes))
    @posts = @posts.order('created_at desc').paginate(pagination)
    render json: @posts, include: params[:include] || [], pagination: pagination_info
  end

  def show
    render json: Post.find(params[:id]), include: params[:include] || []
  end

  def create
    @post = Post.create! params.permit(:author_id, :title, :body, :likes)
    render json: @post, include: []
  end

  def update
    @post = Post.find params[:id]
    @post.update! params.permit(:author_id, :title, :body, :likes)
    render json: @post, include: []
  end

  def destroy
    @post = Post.find(params[:id])
    @post.destroy
    render json: post, include: []
  end

end
