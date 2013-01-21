module Jekyll
  class HexTag < Liquid::Tag
    include TemplateWrapper

    def initialize(tag_name, params, tokens)
      super
      @hex_parts = params.split(' ')
    end

    def render(context)
      source = "<pre>#{all_parts}</pre>"
      safe_wrap(source)
    end

    def all_parts
      letters = @hex_parts.flat_map do |part|
        if part.include? ':'
          color, part = part.split(':')
        else
          color = nil
        end

        part.each_char.map { |c| Part.new(c, color) }
      end

      letters.each_slice(4).map { |letter_chunk| letter_chunk.join }.join(' ')
    end

    class Part
      COLORS = ['red', '#8FF', '#CC0', '#0C0', '#F80']

      def initialize(str, color = 0)
        @str = str
        @color = color.to_i
      end

      def to_s
        if @color == 0
          @str
        else
          color = COLORS[@color - 1]
          "<span style='color: #{color}'>#{@str}</span>"
        end
      end
    end
  end
end

Liquid::Template.register_tag('hex', Jekyll::HexTag)
