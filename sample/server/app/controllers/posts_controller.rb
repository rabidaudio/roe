class PostsController < ApplicationController

  def index
    @posts = Post.where permitted
    @posts = @posts.order('created_at desc').paginate(pagination)
    render json: @posts, include: params[:include] || [], pagination: pagination_info
  end

  def show
    render json: Post.find(params[:id]), include: params[:include] || []
  end

  def create
    @post = Post.create! permitted
    render json: @post, include: []
  end

  def update
    @post = Post.find params[:id]
    @post.update! permitted
    render json: @post, include: []
  end

  def destroy
    @post = Post.find(params[:id])
    @post.destroy
    render json: post, include: []
  end

  private
  def permitted
    permitted = params.permit(:title, :body, :likes)
    permitted[:author_id] = params[:author][:id] unless params[:author].nil?
    permitted
  end
end
